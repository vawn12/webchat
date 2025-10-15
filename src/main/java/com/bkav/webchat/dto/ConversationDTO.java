package com.bkav.webchat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ConversationDTO {
    private Integer id;
    private String name;
    private String type;
    private AccountDTO createdBy;
    private LocalDateTime createdAt;

}
