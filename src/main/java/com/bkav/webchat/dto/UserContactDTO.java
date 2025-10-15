package com.bkav.webchat.dto;

import com.bkav.webchat.enumtype.ContactStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserContactDTO {
    private Long contactId;
    private AccountDTO owner;
    private AccountDTO contactUser;
    private ContactStatus status;           // trạng thái: PENDING, ACCEPTED, BLOCKED,...
    private LocalDateTime createdAt;
}
