package com.bkav.webchat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KafkaMessageEvent {
    private String eventType; // "MESSAGE_SENT", "MESSAGE_UPDATE", "MESSAGE_DELETE"
    private Integer conversationId;
    private MessageResponseDTO messageData;
    private Object extraData; // Dùng cho react
}
