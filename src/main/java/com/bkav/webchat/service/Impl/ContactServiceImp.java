package com.bkav.webchat.service.Impl;

import com.bkav.webchat.dto.ChatListDTO;
import com.bkav.webchat.entity.Participants;
import com.bkav.webchat.repository.ChatListRepository;
import com.bkav.webchat.repository.ParticipationRepository;
import com.bkav.webchat.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContactServiceImp implements ContactService {
    @Autowired
    private ChatListRepository chatListRepository;
    @Autowired
    private ParticipationRepository participationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    public List<ChatListDTO> getChatList(Long userId) {
        List<Object[]> rows = chatListRepository.findChatListByUserId(userId);
        return rows.stream().map(r -> ChatListDTO.builder()
                .conversationId(((Number) r[0]).intValue())
                .title((String) r[1])
                .type((String) r[2])
                .lastMessagePreview((String) r[3])
                .lastMessageTime((LocalDateTime) r[4])
                .unreadCount(r[5] == null ? 0L : ((Number) r[5]).longValue())
                .build()
        ).toList();
    }


    public void notifyChatUpdate(Long conversationId) {
        List<Participants> members = participationRepository.findByConversation_ConversationId(conversationId);
        for (Participants p : members) {
            Integer userId = p.getAccount().getAccountId();
            messagingTemplate.convertAndSend("/topic/chat-list/" + userId,
                    new ChatListUpdateEvent(conversationId));
//            firebasePushService.notifyChatBump(userId, conversationId);
        }
    }

    public record ChatListUpdateEvent(Long conversationId) {

    }

}
