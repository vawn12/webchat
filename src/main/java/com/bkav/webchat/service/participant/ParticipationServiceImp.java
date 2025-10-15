package com.bkav.webchat.service.participant;

import com.bkav.webchat.dto.ParticipationDTO;
import com.bkav.webchat.entity.Conversation;
import com.bkav.webchat.entity.Participation;
import com.bkav.webchat.service.account.AccountService;
import com.bkav.webchat.service.conversation.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ParticipationServiceImp {
    @Autowired
    private ConversationService conversationService;
    @Autowired
    private AccountService accountService;
    public ParticipationDTO convertToDTO(Participation participation) {
        return ParticipationDTO.builder()
                .participantId(participation.getParticipantId())
                .conversation(conversationService.toDTO(participation.getConversation()))
                .account(accountService.convertToDTO(participation.getAccount()))
                .role(participation.getRole())
                .joinedAt(participation.getJoinedAt())
                .build();
    }

    public Participation convertToEntity(ParticipationDTO dto) {
        return Participation.builder()
                .participantId(dto.getParticipantId())
                .conversation(conversationService.ToEntity(dto.getConversation()))
                .account(accountService.convertToEntity(dto.getAccount()))
                .role(dto.getRole())
                .joinedAt(dto.getJoinedAt())
                .build();
    }

}
