package com.bkav.webchat.service;
import com.bkav.webchat.dto.KafkaMessageEvent;
import com.bkav.webchat.dto.MessageResponseDTO;
import com.bkav.webchat.entity.MessageDocument;
import com.bkav.webchat.repository.MessageSearchRepository;
import com.bkav.webchat.repository.ParticipationRepository;
import com.bkav.webchat.service.Impl.MessageServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
public class ChatKafkaConsumer {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessageSearchRepository messageSearchRepository;
    @Autowired
    private ParticipationRepository participationRepository;
    @Autowired
    private FirebasePushService  firebasePushService;


    @KafkaListener(topics = "chat-events", groupId = "webchat")
    public void consumeChatEvent(KafkaMessageEvent event) {
        String topicDestination = "/topic/conversation/" + event.getConversationId();

        // Ưu tiên lấy extraData (cho react, delete...), nếu null thì lấy messageData (cho send, update)
        Object socketPayloadData = (event.getExtraData() != null)
                ? event.getExtraData()
                : event.getMessageData();

        var wsPayload = Map.of(
                "event", event.getEventType(),
                "conversationId", event.getConversationId(),
                "data", socketPayloadData
        );

        // Gửi tới Frontend
        messagingTemplate.convertAndSend(topicDestination, wsPayload);
        //xử lý đồng bộ elsticsearch
        try {
            switch (event.getEventType()) {
                case "MESSAGE_SENT":
                case "FILE_SENT":
                case "MESSAGE_UPDATE":
                    if (event.getMessageData() != null) {
                        MessageDocument doc = toDocument(event.getMessageData());
                        messageSearchRepository.save(doc);
                        System.out.println(" Đã sync messageId " + doc.getMessageId() + " vào ES.");
                    }
                    break;

                case "MESSAGE_DELETE":
                    // Lấy messageId từ extraData để xóa
                    if (event.getExtraData() instanceof Map) {
                        Map data = (Map) event.getExtraData();
                        Integer msgId = (Integer) data.get("messageId");
                        if (msgId != null) {
                            messageSearchRepository.deleteById(msgId);
                            System.out.println(" Đã xóa messageId " + msgId + " khỏi ES.");
                        }
                    }
                    break;

                default:

                    break;
            }
        } catch (Exception e) {
            System.err.println("Lỗi đồng bộ Elasticsearch: " + e.getMessage());
            e.printStackTrace();
        }
        if ("MESSAGE_SENT".equals(event.getEventType()) || "FILE_SENT".equals(event.getEventType())) {
            handlePushNotification(event);
        }
    }
    private void handlePushNotification(KafkaMessageEvent event) {
        try {
            MessageResponseDTO msg = event.getMessageData();
            if (msg == null) return;

            Integer senderId = msg.getSenderId();
            Integer conversationId = msg.getConversationId();

            // Lấy danh sách người nhận (trừ người gửi) trong cuộc hội thoại
            List<Integer> recipientIds = participationRepository.findUserIdsByConversationIdExcludingSender(conversationId, senderId);

            String notificationContent = "FILE_SENT".equals(event.getEventType()) ? "Đã gửi một tệp đính kèm 📎" : msg.getContent();
            // Cần lấy tên người gửi, ở đây giả sử senderId map ra tên hoặc FE tự xử lý
            String senderName = "Tin nhắn mới";

            // Gửi cho từng người nhận
            for (Integer recipientId : recipientIds) {
                firebasePushService.notifyChatBump(recipientId, Long.valueOf(conversationId), senderName, notificationContent);
            }

        } catch (Exception e) {
            System.err.println("Lỗi gửi Push Notification: " + e.getMessage());
        }
    }
    private MessageDocument toDocument(MessageResponseDTO dto) {
        LocalDateTime localDateTime = LocalDateTime.parse(dto.getCreatedAt());

        return MessageDocument.builder()
                .messageId(dto.getMessageId())
                .content(dto.getContent())
                .messageType(dto.getMessageType())
                .conversationId(dto.getConversationId())
                .senderId(dto.getSenderId())
                .createdAt(localDateTime.atZone(ZoneId.systemDefault()).toInstant())
                .build();
    }
}
