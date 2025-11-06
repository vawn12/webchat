package com.bkav.webchat.dto;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponseDTO {
    private Integer messageId;
    private Integer conversationId;
    private Integer senderId;
    private String content;
    private String messageType;
    private String createdAt;
}

