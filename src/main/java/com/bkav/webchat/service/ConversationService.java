package com.bkav.webchat.service;

import com.bkav.webchat.dto.m.ConversationDTO;
import com.bkav.webchat.entity.Conversation;

import java.util.List;

public interface ConversationService {
    List<ConversationDTO> getConversationsByToken(String authorizationHeader);
    List<ConversationDTO> getConversationsByType(String authorizationHeader, String type);
    ConversationDTO createGroupConversation(String authorizationHeader, String name, List<Integer> participantIds);
    ConversationDTO addMembersToGroup(String authorizationHeader, Integer conversationId, List<Integer> newMemberIds);
}
