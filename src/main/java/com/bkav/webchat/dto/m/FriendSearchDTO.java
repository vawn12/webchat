package com.bkav.webchat.dto.m;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

// FriendSearchDTO.java
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FriendSearchDTO {
    @JsonProperty("Content")
    private String content;

    @JsonProperty("Files")
    private List<String> files;

    @JsonProperty("CreatedAt")
    private LocalDateTime createdAt;

    @JsonProperty("Images")
    private List<String> images;

    @JsonProperty("FriendID")
    private String friendID;

    @JsonProperty("FullName")
    private String fullName;

    @JsonProperty("Username")
    private String username;

    private int unreadCount;

    @JsonProperty("UpdateAtUser")
    private LocalDateTime updateAtUser;
}


