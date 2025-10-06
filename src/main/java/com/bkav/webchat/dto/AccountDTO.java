package com.bkav.webchat.dto;

import com.bkav.webchat.enumtype.Account_status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDTO {

    private Integer accountId;
    private String password;
    private Long username;
    private String email;
    private String displayName;
    private String avatarUrl;
    private Account_status status;
    private LocalDateTime createdAt;

}
