package com.bkav.webchat.controller;

import com.bkav.webchat.cache.RedisService;
import com.bkav.webchat.dto.*;
import com.bkav.webchat.dto.m.AccountDTO;
import com.bkav.webchat.dto.m.ForgotPasswordDTO;
import com.bkav.webchat.dto.m.VerifyTokenDTO;
import com.bkav.webchat.enumtype.Account_status;
import com.bkav.webchat.service.AccountService;
import com.bkav.webchat.service.EmailService;
import com.bkav.webchat.service.VerifyTokenService;
import com.bkav.webchat.service.Impl.ForgotService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
//@Tag(name = "Authentication API", description = "Các API xác thực người dùng (login, register, verify, forgot password)")
public class AuthenControllerRest {

    private final AccountService accountService;
    private final ForgotService forgotService;
    private final EmailService emailService;
    private final VerifyTokenService verifyTokenService;
    private final RedisService redisService;

    @Value("${app.base-url}")
    private String baseUrl;

//    @Operation(summary = "Đăng nhập tài khoản")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        var account = accountService.login(username, password);
        if (account == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("Sai tên tài khoản hoặc mật khẩu"));
        }

        AccountResponse response = AccountResponse.builder()
                .id(account.getAccountId())
                .email(account.getEmail())
                .displayName(account.getDisplayName())
                .avatarUrl(account.getAvatarUrl())
                .status(account.getStatus().name())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }


//    @Operation(summary = "Đăng ký tài khoản mới và gửi email xác minh")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        String fullName = request.get("fullname");

        var existing = accountService.findAccountByEmail(email);
        if (existing != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.fail("Email đã được đăng ký"));
        }
        VerifyTokenDTO oldToken = verifyTokenService.getTokenByEmail(email.trim());
        if (oldToken != null) {
            verifyTokenService.deleteTokenByEmail(email.trim());
        }


        String token = UUID.randomUUID().toString();
        VerifyTokenDTO verifyToken = VerifyTokenDTO.builder()
                .email(email)
                .token(token)
                .expiresAt(new Date(System.currentTimeMillis() + 10 * 60 * 1000))
                .fullName(fullName)
                .password(password)
                .build();

        verifyTokenService.create(verifyToken);

        String verifyLink = baseUrl + "/api/auth/verify?token=" + token;
        emailService.sendMailTime(email, "Xác minh tài khoản",
                emailService.buildEmailContent(fullName, verifyLink));

        return ResponseEntity.ok(ApiResponse.success(
                "Đăng ký thành công. Vui lòng kiểm tra email để xác minh tài khoản.",
                Map.of("verifyLink", verifyLink)
        ));
    }


//    @Operation(summary = "Xác minh email thông qua token được gửi qua email")
    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<?>> verify(@RequestParam("token") String token) {
        var verifyToken = verifyTokenService.findByToken(token);
        if (verifyToken == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail("Token không hợp lệ"));
        }

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


//    @Operation(summary = "Gửi mã OTP đặt lại mật khẩu đến email")
    @PostMapping("/forgot")
    public ResponseEntity<ApiResponse<?>> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        var account = accountService.findAccountByEmail(email);
        if (account == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("Email không tồn tại"));
        }

        String otp = generateOtp();
        ForgotPasswordDTO forgot = ForgotPasswordDTO.builder()
                .accountId(account.getAccountId())
                .token(otp)
                .expiryDate(new Date(System.currentTimeMillis() + 5 * 60 * 1000))
                .build();

        forgotService.createForgotPassword(forgot);
        emailService.sendMailTime(email, "Mã OTP đặt lại mật khẩu",
                emailService.builEmailContentForResetPassword(List.of()));

        return ResponseEntity.ok(ApiResponse.success("Đã gửi mã OTP qua email", null));
    }


//    @Operation(summary = "Đặt lại mật khẩu bằng OTP")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<?>> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");

        var account = accountService.findAccountByEmail(email);
        var forgot = forgotService.findForgotPasswordByAccountId(account.getAccountId());

        if (forgot == null || !forgot.getToken().equals(otp)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail("OTP không hợp lệ"));
        }

        if (forgot.getExpiryDate().before(new Date())) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(ApiResponse.fail("OTP đã hết hạn"));
        }

        accountService.register(account, newPassword);
        forgotService.deleteForgotPassword(account.getAccountId());

        return ResponseEntity.ok(ApiResponse.success("Đặt lại mật khẩu thành công", null));
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Token không hợp lệ");
        }

        String token = authHeader.substring(7);

        //  lưu token trong Redis → xoá nó khỏi Redis
        redisService.deleteToken(token);

        //  không dùng Redis
        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công"));
    }

    // Tạo mã OTP 6 chữ số
    private String generateOtp() {
        Random rand = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) sb.append(rand.nextInt(10));
        return sb.toString();
    }
}
