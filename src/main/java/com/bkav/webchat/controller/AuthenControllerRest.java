package com.bkav.webchat.controller;

import com.bkav.webchat.dto.request.ForgotRequest;
import com.bkav.webchat.dto.request.LoginRequest;
import com.bkav.webchat.dto.request.RegisterRequest;
import com.bkav.webchat.dto.request.ResetPasswordRequest;
import com.bkav.webchat.dto.response.ApiResponse;
import com.bkav.webchat.service.AuthService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
//@Tag(name = "Authentication API", description = "Các API xác thực người dùng (login, register, verify, forgot password)")
public class AuthenControllerRest {

    private final AuthService authService;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;
    //login with google
    @PostMapping("/login-google")
    public ResponseEntity<ApiResponse<?>> loginWithGoogle(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String fcmToken = request.get("fcmToken");
        return authService.loginWithGoogle(token, googleClientId, fcmToken);
    }
    //login with facebook
    @PostMapping("/login-facebook")
    public ResponseEntity<ApiResponse<?>> loginFacebook(@RequestBody Map<String, String> requestBody) {
        String accessToken = requestBody.get("accessToken");
        String fcmToken = requestBody.get("fcmToken");
        return authService.loginWithFacebook(accessToken, fcmToken);
    }
    @PostMapping("/registerDevice")
    public ResponseEntity<ApiResponse<?>> registerDeviceToken(@RequestBody Map<String, String> request, Principal principal) {
        String fcmToken = request.get("token");
        authService.saveDeviceToken(principal.getName(), fcmToken);
        return ResponseEntity.ok(ApiResponse.success("Lưu token thiết bị thành công", null));
    }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<?>> verify(@RequestParam("token") String token) {
        return authService.verify(token);
    }

    @PostMapping("/forgot")
    public ResponseEntity<ApiResponse<?>> forgotPassword(@RequestBody ForgotRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<?>> resetPassword(@RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(@RequestBody Map<String , String > body) {
        String refreshtoken = body.get("refreshToken");
        return authService.logout(refreshtoken);
    }
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<?>> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        return authService.refresh(refreshToken);
    }

}
