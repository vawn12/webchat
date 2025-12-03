package com.bkav.webchat.dto.request;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private String fcmToken;//dùng lưu fcm
}
