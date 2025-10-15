package com.bkav.webchat.dto;

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
public class ParticipationDTO {
    private Long participantId;
    private ConversationDTO conversation;
    private AccountDTO account;
    private ParticipantRole role;
    private LocalDateTime joinedAt;
}
