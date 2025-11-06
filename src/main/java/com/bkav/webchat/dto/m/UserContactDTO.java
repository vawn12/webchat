package com.bkav.webchat.dto.m;

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
    private Integer contactId;
    private Integer ownerId;
    private String ownerName;
    private Integer contactUserId;
    private String contactUserName;
    private String contactUserAvatar;
    private String status;
    private LocalDateTime createdAt;
}
