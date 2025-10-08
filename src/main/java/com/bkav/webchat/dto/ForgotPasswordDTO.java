package com.bkav.webchat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForgotPasswordDTO {
    private int id;
    private String account;
    private  String token;
    private Date expiryDate;
}
