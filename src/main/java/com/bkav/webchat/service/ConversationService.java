package com.bkav.webchat.service;

import com.bkav.webchat.dto.response.ContactResponseDTO;
import com.bkav.webchat.dto.m.ConversationDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ConversationService {
    List<ConversationDTO> getConversationsByType(String authorizationHeader, String type);
    ConversationDTO createGroupConversation(String authorizationHeader, String name, List<Integer> participantIds);
    ConversationDTO addMembersToGroup(String authorizationHeader, Integer conversationId, List<Integer> newMemberIds);
    Page<ContactResponseDTO> getAllConversation(String token, int page, int size);
    ConversationDTO getConversationDetails(String authorizationHeader, Integer conversationId);
    void leaveGroup(String authorizationHeader, Integer conversationId);
    ConversationDTO createPrivateConversation(String authorizationHeader, Integer friendId);
    ConversationDTO renameGroupConversation(String authorizationHeader, Integer conversationId, String newName);
}
