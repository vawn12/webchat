package com.bkav.webchat.service.Impl;

import com.bkav.webchat.cache.RedisService;
import com.bkav.webchat.dto.m.*;
import com.bkav.webchat.dto.request.*;
import com.bkav.webchat.dto.response.AccountResponse;
import com.bkav.webchat.dto.response.ApiResponse;
import com.bkav.webchat.entity.Account;
import com.bkav.webchat.entity.UserDeviceToken;
import com.bkav.webchat.enumtype.Account_status;
import com.bkav.webchat.repository.AccountRepository;
import com.bkav.webchat.repository.UserDeviceTokenRepository;
import com.bkav.webchat.security.JwtService;
import com.bkav.webchat.service.*;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final JwtService jwtService;
    private final AccountService accountService;
    private final ForgotService forgotService;
    private final EmailService emailService;
    private final VerifyTokenService verifyTokenService;
    private final RedisService redisService;
    private final AccountRepository accountRepository;
    private final UserDeviceTokenRepository userDeviceTokenRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    public ResponseEntity<ApiResponse<?>> login(LoginRequest request) {
        var account = accountService.login(request.getUsername(), request.getPassword());
        if (account == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("Sai tên tài khoản hoặc mật khẩu"));
        if (request.getFcmToken() != null && !request.getFcmToken().isEmpty()) {
            saveDeviceToken(account.getUsername(), request.getFcmToken());
        }
        String accessToken = jwtService.generateAccessToken(account.getUsername());
        String refreshToken = jwtService.generateRefreshToken(account.getUsername());

        // Lưu refreshToken trong Redis
        redisService.saveToken(refreshToken, account.getUsername());

        Map<String, Object> data = Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "user", AccountResponse.builder()
                        .id(account.getAccountId())
                        .email(account.getEmail())
                        .displayName(account.getDisplayName())
                        .avatarUrl(account.getAvatarUrl())
                        .status(account.getStatus().name())
                        .build()
        );

        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", data));
    }

    @Override
    public ResponseEntity<ApiResponse<?>> register(RegisterRequest request) {
        String email = request.getEmail().trim();

        var existing = accountService.findAccountByEmail(email);
        if (existing != null)
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.fail("Email đã được đăng ký"));

        VerifyTokenDTO oldToken = verifyTokenService.getTokenByEmail(email);
        if (oldToken != null)
            verifyTokenService.deleteTokenByEmail(email);

        String token = UUID.randomUUID().toString();
        VerifyTokenDTO verifyToken = VerifyTokenDTO.builder()
                .email(email)
                .token(token)
                .expiresAt(new Date(System.currentTimeMillis() + 10 * 60 * 1000))
                .fullName(request.getFullName())
                .password(request.getPassword())
                .build();

        verifyTokenService.create(verifyToken);

        String verifyLink = baseUrl + "/api/auth/verify?token=" + token;
        emailService.sendMailTime(email, "Xác minh tài khoản",
                emailService.buildEmailContent(request.getFullName(), verifyLink));

        return ResponseEntity.ok(ApiResponse.success(
                "Đăng ký thành công. Vui lòng kiểm tra email để xác minh tài khoản.",
                Map.of("verifyLink", verifyLink)
        ));
    }

    //VERIFY EMAIL
    @Override
    public ResponseEntity<ApiResponse<?>> verify(String token) {
        var verifyToken = verifyTokenService.findByToken(token);
        if (verifyToken == null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail("Token không hợp lệ"));

        if (verifyToken.getExpiresAt().before(new Date())) {
            verifyTokenService.deleteTokenByEmail(verifyToken.getEmail());
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(ApiResponse.fail("Token đã hết hạn"));
        }

        AccountDTO dto = AccountDTO.builder()
                .displayName(verifyToken.getEmail().split("@")[0])
                .email(verifyToken.getEmail())
                .username(verifyToken.getFullName())
                .status(Account_status.offline)
                .build();

        accountService.register(dto, verifyToken.getPassword());
        verifyTokenService.deleteTokenByEmail(verifyToken.getEmail());

        return ResponseEntity.ok(ApiResponse.success("Xác minh email thành công", null));
    }

    //FORGOT PASSWORD
    @Override
    public ResponseEntity<ApiResponse<?>> forgotPassword(ForgotRequest request) {
        var account = accountService.findAccountByEmail(request.getEmail());
        if (account == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("Email không tồn tại"));

        String otp = generateOtp();
        ForgotPasswordDTO forgot = ForgotPasswordDTO.builder()
                .accountId(account.getAccountId())
                .token(otp)
                .expiryDate(new Date(System.currentTimeMillis() + 5 * 60 * 1000))
                .build();

        forgotService.createForgotPassword(forgot);
        emailService.sendMailTime(request.getEmail(), "Mã OTP đặt lại mật khẩu",
                emailService.builEmailContentForResetPassword(List.of()));

        return ResponseEntity.ok(ApiResponse.success("Đã gửi mã OTP qua email", null));
    }

    //RESET PASSWORD
    @Override
    public ResponseEntity<ApiResponse<?>> resetPassword(ResetPasswordRequest request) {
        var account = accountService.findAccountByEmail(request.getEmail());
        var forgot = forgotService.findForgotPasswordByAccountId(account.getAccountId());

        if (forgot == null || !forgot.getToken().equals(request.getOtp()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail("OTP không hợp lệ"));

        if (forgot.getExpiryDate().before(new Date()))
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(ApiResponse.fail("OTP đã hết hạn"));

        accountService.register(account, request.getNewPassword());
        forgotService.deleteForgotPassword(account.getAccountId());

        return ResponseEntity.ok(ApiResponse.success("Đặt lại mật khẩu thành công", null));
    }

    //LOGOUT
    @Override
    public ResponseEntity<ApiResponse<?>> logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail("Refresh token không được để trống."));
        }

        // Kiểm tra token có tồn tại trong Redis không
        if (!redisService.isTokenValid(refreshToken)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail("Refresh token không hợp lệ hoặc đã bị thu hồi."));
        }
        // Lấy username để xóa cache user
        String username = redisService.getUsernameFromToken(refreshToken);
        // Xóa token khỏi Redis
        redisService.deleteToken(refreshToken);

        //xóa cache user để force login lại
        if (username != null) {
            redisService.deleteUserCache(username);
        }

        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
    }

    @Override
    public ResponseEntity<ApiResponse<?>> refresh(String refreshToken) {
        if (refreshToken == null || !redisService.isTokenValid(refreshToken)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail("Refresh token không hợp lệ hoặc đã bị thu hồi."));
        }

        String username = jwtService.extractUsername(refreshToken);

        // Sinh token mới
        String newAccess = jwtService.generateAccessToken(username);
        String newRefresh = jwtService.generateRefreshToken(username);

        // Thu hồi token cũ và lưu lại token mới
        redisService.deleteToken(refreshToken);
        redisService.saveToken(newRefresh, username);

        Map<String, Object> data = Map.of(
                "accessToken", newAccess,
                "refreshToken", newRefresh
        );

        return ResponseEntity.ok(ApiResponse.success("Làm mới token thành công.", data));
    }
    //đăng nhập bằng google
    @Override
    public ResponseEntity<ApiResponse<?>> loginWithGoogle(String googleToken, String clientId,String fcmToken) {
        try {
            // Verify Token với Google
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(clientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(googleToken);
            if (idToken == null) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Token Google không hợp lệ"));
            }
            // Lấy thông tin
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            // Tìm hoặc Tạo User
            Account account = accountRepository.findByEmail(email).orElse(null);
            if (account == null) {
            String baseUsername = email.split("@")[0];
            String finalUsername = baseUsername;
            int suffix = 1;
            while (accountRepository.existsByUsername(finalUsername)) {
                // Nếu trùng thì thêm số đằng sau
                finalUsername = baseUsername + "_" + suffix;
                suffix++;
            }
                account = new Account();
                account.setEmail(email);
                account.setDisplayName(name);
                account.setAvatarUrl(pictureUrl);
                account.setStatus(Account_status.online);
                account.setPasswordHash("");

                account.setUsername(finalUsername);
                account = accountRepository.save(account);
            }
            if (fcmToken != null && !fcmToken.isEmpty()) {
                saveDeviceToken(account.getUsername(), fcmToken);
            }
            //  Tạo JWT
            String accessToken = jwtService.generateAccessToken(account);
            String refreshToken = jwtService.generateRefreshToken(account);

            //  Trả về kết quả

            Map<String, Object> data = new HashMap<>();
            data.put("accessToken", accessToken);
            data.put("refreshToken", refreshToken);
            data.put("userId", account.getAccountId());
            data.put("username", account.getDisplayName());
            data.put("avatarUrl", account.getAvatarUrl());

            return ResponseEntity.ok(ApiResponse.success("Đăng nhập Google thành công", data));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Lỗi xác thực Google: " + e.getMessage()));
        }
    }
    // login bằng facebook
    @Override
    public ResponseEntity<ApiResponse<?>> loginWithFacebook(String accessToken, String fcmToken) {
        try {
            //  Gọi Facebook Graph API để lấy thông tin user từ accessToken
            String facebookGraphUrl = "https://graph.facebook.com/me?fields=id,name,email,picture&access_token=" + accessToken;

            RestTemplate restTemplate = new RestTemplate();
            // Gọi API, kết quả trả về map object
            Map<String, Object> fbResponse = restTemplate.getForObject(facebookGraphUrl, Map.class);

            if (fbResponse == null || fbResponse.containsKey("error")) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Token Facebook không hợp lệ hoặc đã hết hạn"));
            }

            // Lấy thông tin từ response của Facebook
            String email = (String) fbResponse.get("email");
            String name = (String) fbResponse.get("name");
            String facebookId = (String) fbResponse.get("id");

            // Xử lý lấy ảnh đại diện
            String pictureUrl = null;
            if (fbResponse.get("picture") instanceof Map) {
                Map<String, Object> pictureObj = (Map<String, Object>) fbResponse.get("picture");
                if (pictureObj.get("data") instanceof Map) {
                    Map<String, Object> dataObj = (Map<String, Object>) pictureObj.get("data");
                    pictureUrl = (String) dataObj.get("url");
                }
            }

            // Trường hợp Facebook không trả về email (do user đăng ký bằng sđt)
            // sẽ tạo một email giả lập dựa trên Facebook ID để lưu vào DB
            if (email == null || email.isEmpty()) {
                email = facebookId + "@facebook.com";
            }

            // Tìm hoặc Tạo User (Logic tương tự Google Login)
            Account account = accountRepository.findByEmail(email).orElse(null);

            if (account == null) {
                // Xử lý trùng username
                String baseUsername = email.split("@")[0];
                // Loại bỏ các ký tự đặc biệt khỏi username nếu có
                baseUsername = baseUsername.replaceAll("[^a-zA-Z0-9]", "");

                String finalUsername = baseUsername;
                int suffix = 1;
                while (accountRepository.existsByUsername(finalUsername)) {
                    finalUsername = baseUsername + "_" + suffix;
                    suffix++;
                }

                account = new Account();
                account.setEmail(email);
                account.setDisplayName(name);
                account.setAvatarUrl(pictureUrl);
                account.setStatus(Account_status.online);
                account.setPasswordHash(""); // Không có password
                account.setUsername(finalUsername);

                account = accountRepository.save(account);
            }

            // Lưu device token nếu có
            if (fcmToken != null && !fcmToken.isEmpty()) {
                saveDeviceToken(account.getUsername(), fcmToken);
            }

            // Tạo JWT AccessToken và RefreshToken
            String newAccessToken = jwtService.generateAccessToken(account);
            String newRefreshToken = jwtService.generateRefreshToken(account);

            // Lưu refresh token vào Redis
            redisService.saveToken(newRefreshToken, account.getUsername());

            // Kết quả
            Map<String, Object> data = new HashMap<>();
            data.put("accessToken", newAccessToken);
            data.put("refreshToken", newRefreshToken);
            data.put("userId", account.getAccountId());
            data.put("username", account.getDisplayName());
            data.put("avatarUrl", account.getAvatarUrl());

            return ResponseEntity.ok(ApiResponse.success("Đăng nhập Facebook thành công", data));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Lỗi xác thực Facebook: " + e.getMessage()));
        }
    }
    //	lưu token của thiết bị được đăng nhập
    public void saveDeviceToken(String username, String fcmToken) {
        //  Lấy thông tin người dùng đang đăng nhập
        Account currentUser = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        //  Kiểm tra token này đã có trong DB chưa
        Optional<UserDeviceToken> existingTokenOpt = userDeviceTokenRepository.findByToken(fcmToken);
        if (existingTokenOpt.isPresent()) {
            // case: Token đã tồn tại (Thiết bị này đã từng đăng nhập)
            UserDeviceToken existingToken = existingTokenOpt.get();
            // Cập nhật lại Account là người dùng hiện tại
            existingToken.setAccount(currentUser);
            // Cập nhật thời gian để biết token còn hoạt động
            existingToken.setLastUpdated(LocalDateTime.now());
            userDeviceTokenRepository.save(existingToken);
        } else {
            // TRƯỜNG HỢP: Token mới hoàn toàn -> Tạo mới
            UserDeviceToken newToken = UserDeviceToken.builder()
                    .token(fcmToken)
                    .account(currentUser)
                    .lastUpdated(LocalDateTime.now())
                    .build();
            userDeviceTokenRepository.save(newToken);
        }
    }

    private String generateOtp() {
        Random rand = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) sb.append(rand.nextInt(10));
        return sb.toString();
    }
}
