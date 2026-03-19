package com.bkav.webchat.dto.request;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String username;
    private String password;
    private String fullName;
}
