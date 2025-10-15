package com.bkav.webchat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatListDTO {
    private Integer conversationId;
    private String title;
    private String type;
    private String avatarUrl;
    private String lastMessagePreview;
    private LocalDateTime lastMessageTime;
    private Long unreadCount;          // số tin chưa đọc cho user này
    private boolean muted;
}
