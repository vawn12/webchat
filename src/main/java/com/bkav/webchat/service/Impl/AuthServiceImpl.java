package com.bkav.webchat.service.Impl;

import com.bkav.webchat.cache.RedisService;
import com.bkav.webchat.dto.*;
import com.bkav.webchat.dto.m.*;
import com.bkav.webchat.dto.request.*;
import com.bkav.webchat.enumtype.Account_status;
import com.bkav.webchat.security.JwtService;
import com.bkav.webchat.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

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

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    public ResponseEntity<ApiResponse<?>> login(LoginRequest request) {
        var account = accountService.login(request.getUsername(), request.getPassword());
        if (account == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("Sai tên tài khoản hoặc mật khẩu"));

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
                .status(Account_status.OFFLINE)
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


    private String generateOtp() {
        Random rand = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) sb.append(rand.nextInt(10));
        return sb.toString();
    }
}
