package com.bkav.webchat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContactResponse {
    private Integer id;
    private String type;
    private String file;
    private String image;
    //Thông tin cho bạn bè
    private Integer contactUserId;
    private String userName;
    private String fullName;
    private String contactUserAvatar;
    private String userStatus;

    //Tin nhắn cuối
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private Long unreadCount;
}
