package com.bkav.webchat.dto.m;

import com.bkav.webchat.enumtype.ParticipantRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantDTO {
    private Integer accountId;
    private String displayName;
    private String avatarUrl;
    private ParticipantRole role;
}
