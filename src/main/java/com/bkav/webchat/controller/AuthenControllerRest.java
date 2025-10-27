package com.bkav.webchat.controller;

import com.bkav.webchat.cache.RedisService;
import com.bkav.webchat.dto.*;
import com.bkav.webchat.dto.m.AccountDTO;
import com.bkav.webchat.dto.m.ForgotPasswordDTO;
import com.bkav.webchat.dto.m.VerifyTokenDTO;
import com.bkav.webchat.dto.request.ForgotRequest;
import com.bkav.webchat.dto.request.LoginRequest;
import com.bkav.webchat.dto.request.RegisterRequest;
import com.bkav.webchat.dto.request.ResetPasswordRequest;
import com.bkav.webchat.enumtype.Account_status;
import com.bkav.webchat.service.AccountService;
import com.bkav.webchat.service.AuthService;
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

    private final AuthService authService;

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
    public ResponseEntity<ApiResponse<?>> logout(@RequestHeader("Authorization") String authHeader) {
        return authService.logout(authHeader);
    }
}
