package com.bkav.webchat.service;

import com.bkav.webchat.dto.m.ConversationDTO;
import com.bkav.webchat.entity.Conversation;

public interface ConversationService {
    Conversation ToEntity(ConversationDTO dto);
    ConversationDTO toDTO(Conversation conversation);
}
