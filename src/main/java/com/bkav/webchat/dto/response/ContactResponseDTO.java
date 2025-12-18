package com.bkav.webchat.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactResponseDTO {

    private Integer id;
    private String type;

    //Thông tin cho group
    private String name;
    private String avatarUrl;

    //Thông tin cho bạn bè
    private Integer contactUserId;
    private String contactUserName;
    private String contactUserAvatar;
    private String userStatus;

    //Tin nhắn cuối
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private Long unreadCount;
}
