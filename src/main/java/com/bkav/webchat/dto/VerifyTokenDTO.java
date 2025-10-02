package com.bkav.webchat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VerifyTokenDTO {
    private Integer id;

    private String email;

    private String token;

    private Date expiresAt;

    private String password;

    private String fullName;
}
