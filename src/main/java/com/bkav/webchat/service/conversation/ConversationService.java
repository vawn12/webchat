package com.bkav.webchat.service.conversation;

import com.bkav.webchat.dto.ConversationDTO;
import com.bkav.webchat.entity.Conversation;

public interface ConversationService {
    Conversation ToEntity(ConversationDTO dto);
    ConversationDTO toDTO(Conversation conversation);
}
