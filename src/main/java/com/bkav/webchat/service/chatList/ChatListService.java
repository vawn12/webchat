//package com.bkav.webchat.service.chatList;
//
//import com.bkav.webchat.dto.ChatListDTO;
//import com.bkav.webchat.entity.Participation;
//import com.bkav.webchat.repository.ChatListRepository;
//import com.bkav.webchat.repository.ParticipationRepository;
//import com.bkav.webchat.service.participant.ParticipationService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Service
//public class ChatListService {
//    @Autowired
//    private ChatListRepository chatListRepository;
//    @Autowired
//    private ParticipationRepository participationRepository;
//
//    public List<ChatListDTO> getChatList(Long userId) {
//        List<Object[]> rows = chatListRepository.findChatListByUserId(userId);
//        return rows.stream().map(r -> ChatListDTO.builder()
//                .conversationId(((Number) r[0]).intValue())
//                .title((String) r[1])
//                .type((String) r[2])
//                .lastMessagePreview((String) r[3])
//                .lastMessageTime((LocalDateTime) r[4])
//                .unreadCount(r[5] == null ? 0L : ((Number) r[5]).longValue())
//                .build()
//        ).toList();
//    }
//
////    @Override
////    public void notifyChatUpdate(Long conversationId) {
////        List<Participation> members = participationRepository.findByConversation_ConversationId(conversationId);
////        for (Participation p : members) {
////            Integer userId = p.getAccount().getAccountId();
////            messagingTemplate.convertAndSend("/topic/chat-list/" + userId,
////                    new ChatListUpdateEvent(conversationId));
////            firebasePushService.notifyChatBump(userId, conversationId);
////        }
////    }
//
//    public record ChatListUpdateEvent(Long conversationId) {}
//}
