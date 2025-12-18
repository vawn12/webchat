package com.bkav.webchat.dto.response;
import lombok.*;

import java.util.Map;

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

    private Map<String, Object> metadata;
}

