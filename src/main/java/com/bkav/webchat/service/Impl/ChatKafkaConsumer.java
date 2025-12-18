package com.bkav.webchat.service.Impl;
import com.bkav.webchat.cache.RedisService;
import com.bkav.webchat.dto.response.KafkaMessageEvent;
import com.bkav.webchat.dto.response.MessageResponseDTO;
import com.bkav.webchat.entity.Account;
import com.bkav.webchat.entity.MessageDocument;
import com.bkav.webchat.repository.AccountRepository;
import com.bkav.webchat.repository.MessageRepository;
import com.bkav.webchat.repository.MessageSearchRepository;
import com.bkav.webchat.repository.ParticipationRepository;
import com.bkav.webchat.service.FirebasePushService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatKafkaConsumer {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessageSearchRepository messageSearchRepository;
    @Autowired
    private ParticipationRepository participationRepository;
    @Autowired
    private FirebasePushService firebasePushService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private RedisService redisService;


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

                        // SỬA ĐOẠN NÀY: Hứng kết quả trả về từ hàm save
                        MessageDocument savedDoc = messageSearchRepository.save(doc);

                        System.out.println("======== DEBUG ELASTICSEARCH ========");
                        System.out.println("Original Message ID: " + doc.getMessageId());
                        // Nếu savedDoc.getMessageId() null hoặc khác ID gốc => Thiếu @Id
                        System.out.println("Saved ID in ES: " + savedDoc.getMessageId());
                        System.out.println("=====================================");
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

            // Lấy danh sách người nhận
            List<Integer> recipientIds = participationRepository.findUserIdsByConversationIdExcludingSender(conversationId, senderId);
            if (recipientIds.isEmpty()) return;

            // Lấy thông tin Account
            List<Account> recipients = accountRepository.findAllById(recipientIds);
            List<String> usernames = recipients.stream().map(Account::getUsername).collect(Collectors.toList());

            // Check Online hàng loạt
            List<String> onlineUsernames = redisService.getOnlineUsers(usernames);

            // Nhóm 1: Nhận tin chi tiết (Unread = 1 hoặc mới hết cooldown)
            List<Integer> groupSendDetail = new ArrayList<>();

            // Nhóm 2: Nhận tin tổng hợp -> map cả số lượng v danh sách user
            Map<Long, List<Integer>> summaryGroups = new HashMap<>();
            for (Account recipient : recipients) {
                // Nếu Online -> Bỏ qua
                if (onlineUsernames.contains(recipient.getUsername())) {
                    redisService.resetUnreadNotification(recipient.getAccountId());
                    System.out.println(">>> User " + recipient.getUsername() + " đang Online: BỎ QUA Push.");
                    continue;
                }
                Integer userId = recipient.getAccountId();
                // Tăng biến đếm và lấy giá trị hiện tại
                long unreadCount = redisService.incrementUnreadNotification(userId);

                // Check Cooldown
                boolean canSend = redisService.checkAndSetPushCooldown(userId, 2);
                if (canSend) {
                    if (unreadCount > 1) {
                        // Gom nhóm theo số lượng tin nhắn
                        // Nếu chưa có nhóm 'unreadCount' thì tạo mới, rồi thêm user vào
                        summaryGroups.computeIfAbsent(unreadCount, k -> new ArrayList<>()).add(userId);
                    } else {
                        // Nếu mới có 1 tin -> Gửi chi tiết
                        groupSendDetail.add(userId);
                    }
                }
            }
            // Gửi batch đến firebaase
            String senderName = "Tin nhắn mới";
            Optional<Account> senderOpt = accountRepository.findById(senderId);
            if (senderOpt.isPresent()) senderName = senderOpt.get().getDisplayName();

            // Gửi nhóm 1: Nội dung chi tiết
            if (!groupSendDetail.isEmpty()) {
                String content = "FILE_SENT".equals(event.getEventType()) ? "Đã gửi một tệp đính kèm 📎" : msg.getContent();
                firebasePushService.sendNotificationToGroup(groupSendDetail, Long.valueOf(conversationId), senderName, content);
                System.out.println(">>> Đã GỬI CHI TIẾT tới các ID: " + groupSendDetail);
            }

            // Gửi nhóm 2: Nội dung tổng hợp
            // Duyệt qua từng nhóm số lượng để gửi
            for (Map.Entry<Long, List<Integer>> entry : summaryGroups.entrySet()) {
                Long count = entry.getKey();         // Ví dụ: 5
                List<Integer> users = entry.getValue(); // Danh sách user có 5 tin nhắn

                String title = "Thông báo mới";
                String summaryContent = "Có " + count + " tin nhắn chưa đọc";

                // Gửi 1 lần cho cả list user có cùng số lượng tin
                firebasePushService.sendNotificationToGroup(users, Long.valueOf(conversationId), title, summaryContent);
                System.out.println(">>> Đã GỬI TỔNG HỢP (" + count + " tin) tới các ID: " + users);
            }

        } catch (Exception e) {
            System.err.println("Lỗi xử lý Push: " + e.getMessage());
            e.printStackTrace();
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
