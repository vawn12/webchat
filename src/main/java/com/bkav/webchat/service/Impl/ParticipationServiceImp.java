package com.bkav.webchat.service.Impl;

import com.bkav.webchat.dto.m.ParticipantDTO;
import com.bkav.webchat.entity.Participants;
import com.bkav.webchat.service.AccountService;
import com.bkav.webchat.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ParticipationServiceImp {
    @Autowired
    private ConversationService conversationService;
    @Autowired
    private AccountService accountService;
//    public ParticipantDTO convertToDTO(Participants participation) {
//        return ParticipantDTO.builder()
//                .participantId(participation.getParticipantId())
//                .conversation(conversationService.toDTO(participation.getConversation()))
//                .account(accountService.convertToDTO(participation.getAccount()))
//                .role(participation.getRole())
//                .joinedAt(participation.getJoinedAt())
//                .build();
//    }
//
//    public Participants convertToEntity(ParticipantDTO dto) {
//        return Participants.builder()
//                .participantId(dto.getParticipantId())
//                .conversation(conversationService.ToEntity(dto.getConversation()))
//                .account(accountService.convertToEntity(dto.getAccount()))
//                .role(dto.getRole())
//                .joinedAt(dto.getJoinedAt())
//                .build();
//    }

}
