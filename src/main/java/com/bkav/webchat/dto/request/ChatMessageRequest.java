package com.bkav.webchat.dto.request;

import com.bkav.webchat.dto.m.ConversationDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessageRequest {
    private Integer messageId;
    private String content;
    private String messageType;
    private Integer repliedToMessageId;
    private Integer receiverId;
}
