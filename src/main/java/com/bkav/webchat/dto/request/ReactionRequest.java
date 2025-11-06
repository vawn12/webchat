package com.bkav.webchat.dto.request;

import lombok.Data;

@Data
public class ReactionRequest {
    private Integer messageId;
    private String reactionType;
}
