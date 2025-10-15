package com.bkav.webchat.service.conversation;

import com.bkav.webchat.dto.ConversationDTO;
import com.bkav.webchat.entity.Conversation;
import com.bkav.webchat.repository.ConversationRepository;
import com.bkav.webchat.service.account.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConversationServiceImp implements ConversationService {
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private AccountService accountService;
    public ConversationDTO toDTO(Conversation conversation){
        return ConversationDTO.builder()
                .id(conversation.getConversationId())
                .type(conversation.getType())
                .name(conversation.getName())
                .createdBy(accountService.convertToDTO(conversation.getCreatedBy()))
                .createdAt(conversation.getCreatedAt())
                .build();
    }
    public Conversation ToEntity(ConversationDTO dto) {
        return Conversation.builder()
                .conversationId(dto.getId())
                .name(dto.getName())
                .type(dto.getType())
                .createdBy(accountService.convertToEntity(dto.getCreatedBy()))
                .createdAt(dto.getCreatedAt())
                .build();
    }
}
