package com.bkav.webchat.dto.m;

import com.bkav.webchat.enumtype.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ConversationDTO {
    private Integer conversationId;
    private String name;
    private ConversationType type;
    private String createdBy;
    private LocalDateTime createdAt;
    private List<ParticipantDTO> participants;


}
