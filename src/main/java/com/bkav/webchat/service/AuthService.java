package com.bkav.webchat.service;

import com.bkav.webchat.dto.request.*;
import com.bkav.webchat.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    ResponseEntity<ApiResponse<?>> login(LoginRequest request);
    ResponseEntity<ApiResponse<?>> register(RegisterRequest request);
    ResponseEntity<ApiResponse<?>> verify(String token);
    ResponseEntity<ApiResponse<?>> forgotPassword(ForgotRequest request);
    ResponseEntity<ApiResponse<?>> resetPassword(ResetPasswordRequest request);
    ResponseEntity<ApiResponse<?>> logout(LogoutRequest request);
    ResponseEntity<ApiResponse<?>> refresh(String refreshToken);
    ResponseEntity<ApiResponse<?>> loginWithGoogle(String googleToken, String clientId,String fcmToken);
    void saveDeviceToken(String username, String fcmToken);
    ResponseEntity<ApiResponse<?>> loginWithFacebook(String accessToken, String fcmToken);

}
