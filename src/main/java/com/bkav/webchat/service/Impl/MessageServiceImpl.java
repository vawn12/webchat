package com.bkav.webchat.service.Impl;

import com.bkav.webchat.cache.RedisService;
import com.bkav.webchat.dto.response.ApiResponse;
import com.bkav.webchat.dto.response.KafkaMessageEvent;
import com.bkav.webchat.dto.response.MessageResponseDTO;
import com.bkav.webchat.dto.request.ChatMessageRequest;
import com.bkav.webchat.dto.request.ReactionRequest;
import com.bkav.webchat.entity.*;
import com.bkav.webchat.enumtype.Message_Status;
import com.bkav.webchat.repository.*;
import com.bkav.webchat.service.AccountService;
import com.bkav.webchat.service.MessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service

public class MessageServiceImpl implements MessageService {
    @Autowired
    private UserContactRepository userContactRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private MessageStatusRepository messageStatusRepository;
    @Autowired
    private AccountService accountService;
    @Autowired
    private MessageReactionRepository  messageReactionRepository;
    @Autowired
    private AttachmentRepository attachmentRepository;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private RedisService redisService;
    @Autowired
    private ParticipationRepository participationRepository;
    @Autowired
    private MessageSearchRepository messageSearchRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_CHAT = "chat-events";


    @Getter
    @AllArgsConstructor
    @Data
    @Builder
    public static class MessageSyncEvent {
        private final Message message;
        private final String action; //save hoặc delete

    }


    private Message toEntity(ChatMessageRequest request, Conversation conversation, Account sender) {
        return Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getContent())
                .messageType(request.getMessageType())
//                .isRead(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }


    private MessageResponseDTO toDTO(Message message) {
        Map<String, Object> metadataMap = null;
        if (message.getMetadata() != null && !message.getMetadata().isBlank()) {
            try {
                // Chuyển đổi chuỗi JSON từ DB thành Map
                metadataMap = objectMapper.readValue(message.getMetadata(), new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return MessageResponseDTO.builder()
                .messageId(message.getMessageId())
                .conversationId(message.getConversation().getConversationId())
                .senderId(message.getSender().getAccountId())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt().toString())
                .metadata(metadataMap)
                .build();
    }

    //  Message → MessageStatus
    private MessageStatus toMessageStatus(Message message, Account sender,Message_Status messageStatus) {
        return MessageStatus.builder()
                .message(message)
                .account(sender)
                .status(messageStatus)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    //gửi tin nhắn đồng thời reply lại tin nhắn cũ realtime
    @Override
    @Transactional
    public ApiResponse<MessageResponseDTO> sendMessage(Integer conversationId, ChatMessageRequest request, String username) {
        Optional<Conversation> optionalConversation = conversationRepository.findById(conversationId);
        if (optionalConversation.isEmpty()) {
            return ApiResponse.fail("Không tìm thấy cuộc trò chuyện.");
        }

        // Lấy sender từ username
        var sender = accountService.getAccountEntityByUsername(username);
        if (sender == null) {
            return ApiResponse.fail("Không tìm thấy người gửi .");
        }

        // Tạo và lưu Message Entity
        Message message = toEntity(request, optionalConversation.get(), sender);
        //xử lý reply
        if (request.getRepliedToMessageId() != null) {
            Optional<Message> originalMessageOpt = messageRepository.findById(request.getRepliedToMessageId());

            // reply nếu tin nhắn gốc tồn tại VÀ trong cùng cuộc trò chuyện
            if (originalMessageOpt.isPresent() &&
                    originalMessageOpt.get().getConversation().getConversationId().equals(conversationId)) {

                Message originalMessage = originalMessageOpt.get();

                // Tạo đối tượng Map chứa thông tin reply
                Map<String, Object> replyInfo = Map.of(
                        "repliedToMessageId", originalMessage.getMessageId(),
                        "repliedToSenderId", originalMessage.getSender().getAccountId(),
                        "repliedToSenderName", originalMessage.getSender().getDisplayName(),
                        "repliedToContent", originalMessage.getContent()
                );

                // Tạo metadata chính
                Map<String, Object> metadata = Map.of("replyInfo", replyInfo);

                // Chuyển Map thành chuỗi JSON để lưu vào DB
                try {
                    message.setMetadata(objectMapper.writeValueAsString(metadata));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
        message = messageRepository.save(message);

        // Lưu MessageStatus
        MessageStatus senderStatus = MessageStatus.builder()
                .message(message)
                .account(sender)
                .status(Message_Status.sent)
                .updatedAt(LocalDateTime.now())
                .build();
        messageStatusRepository.save(senderStatus);

        if (request.getReceiverId() != null) {
            Account receiver = accountService.getAccountById(request.getReceiverId());

            if (receiver != null) {
                MessageStatus receiverStatus = MessageStatus.builder()
                        .message(message)
                        .account(receiver)
                        .status(Message_Status.delivered)
                        .updatedAt(LocalDateTime.now())
                        .build();
                messageStatusRepository.save(receiverStatus);
            }
        }

        //kafka
        MessageResponseDTO dto = toDTO(message);

        KafkaMessageEvent event = KafkaMessageEvent.builder()
                .eventType("MESSAGE_SENT")
                .conversationId(conversationId)
                .messageData(dto)
                .build();
        //lưu

        kafkaTemplate.send(TOPIC_CHAT, String.valueOf(conversationId), event);

        // Trả về kết quả ngay lập tức cho người dùng
        return ApiResponse.success("Gửi tin nhắn thành công.", dto);
    }
    //update tin nhắn của nguời gửi
    public ApiResponse<MessageResponseDTO> updateMessage(Integer conversationId, ChatMessageRequest request, String username) {
        Optional<Conversation> optionalConversation = conversationRepository.findById(conversationId);
        if (optionalConversation.isEmpty()) {
            return ApiResponse.fail("Không tìm thấy cuộc trò chuyện.");
        }
        if (request.getMessageId() == null) {
            return ApiResponse.fail("Thiếu messageId trong request.");
        }
        Optional<Message> optionalMessage = messageRepository.findById(request.getMessageId());
        if (optionalMessage.isEmpty()) {
            return ApiResponse.fail("Không tìm thấy tin nhắn cần cập nhật.");
        }
        Message message = optionalMessage.get();

        // Kiểm tra tin nhắn có thuộc cuộc trò chuyện này không
        if (!message.getConversation().getConversationId().equals(conversationId.intValue())) {
            return ApiResponse.fail("Tin nhắn không thuộc cuộc trò chuyện này.");
        }

        //LOGIC KIỂM TRA QUYỀN
        Account currentUser = accountService.getAccountEntityByUsername(username);
        if (currentUser == null) {
            return ApiResponse.fail("Không tìm thấy thông tin người dùng (user).");
        }

        // So sánh ID của người dùng hiện tại với ID của người gửi tin nhắn
        if (!message.getSender().getAccountId().equals(currentUser.getAccountId())) {
            return ApiResponse.fail("Bạn không có quyền sửa tin nhắn của người khác.");
        }

        message.setContent(request.getContent());
        message.setMessageType(request.getMessageType());
        message.setUpdatedAt(LocalDateTime.now());
        Message updated = messageRepository.save(message);


        MessageResponseDTO dto = toDTO(updated);
        Map<String, Object> socketData = Map.of(
                "messageId", updated.getMessageId(),
                "senderId", updated.getSender().getAccountId(),
                "content", updated.getContent(),
                "messageType", updated.getMessageType(),
                "updatedAt", updated.getUpdatedAt().toString()
        );


        KafkaMessageEvent event = KafkaMessageEvent.builder()
                .eventType("MESSAGE_UPDATE")
                .conversationId(conversationId)
                .messageData(dto)
                .extraData(socketData)
                .build();

        kafkaTemplate.send(TOPIC_CHAT, String.valueOf(conversationId), event);

        return ApiResponse.success("Cập nhật thành công.", dto);
    }

    // XÓA TIN NHẮN
    // Chỉ có thể xóa tin nhắn của mình gửi và không thể xóa tin nhắn của đối phương
    @Transactional
    public ApiResponse<Void> deleteMessage(Integer conversationId, ChatMessageRequest request, String username) {
        Optional<Conversation> optionalConversation = conversationRepository.findById(conversationId);
        if (optionalConversation.isEmpty()) {
            return ApiResponse.fail("Không tìm thấy cuộc trò chuyện.");
        }

        if (request.getMessageId() == null) {
            return ApiResponse.fail("Thiếu messageId trong request.");
        }

        Optional<Message> optionalMessage = messageRepository.findById(request.getMessageId());
        if (optionalMessage.isEmpty()) {
            return ApiResponse.fail("Không tìm thấy tin nhắn cần xóa.");
        }

        Message message = optionalMessage.get();
        if (!message.getConversation().getConversationId().equals(conversationId.intValue())) {
            return ApiResponse.fail("Tin nhắn không thuộc cuộc trò chuyện này.");
        }

        // LOGIC KIỂM TRA QUYỀN
        Account currentUser = accountService.getAccountEntityByUsername(username);
        if (currentUser == null) {
            return ApiResponse.fail("Không tìm thấy thông tin người dùng (user).");
        }

        // So sánh ID của người dùng hiện tại với ID của người gửi tin nhắn
        if (!message.getSender().getAccountId().equals(currentUser.getAccountId())) {
            return ApiResponse.fail("Bạn không có quyền xóa tin nhắn của người khác.");
        }
        List<Attachment> attachments = attachmentRepository.findByMessage(message);
        if (attachments != null && !attachments.isEmpty()) {
            attachmentRepository.deleteAll(attachments);
        }

        //  Cập nhật nội dung tin nhắn
        message.setContent("Tin nhắn đã được thu hồi");
        message.setMessageType("recalled");
        message.setUpdatedAt(LocalDateTime.now());
        // Xóa metadata (nếu là tin reply) để tránh hiển thị trích dẫn cũ
        message.setMetadata(null);

        Message updatedMessage = messageRepository.save(message);

        List<MessageStatus> statuses = messageStatusRepository.findAllByMessage(message);
        for (MessageStatus status : statuses) {
            status.setStatus(Message_Status.recalled);
            status.setUpdatedAt(LocalDateTime.now());
        }
        messageStatusRepository.saveAll(statuses);
        MessageResponseDTO dto = toDTO(updatedMessage);

        // Map thêm data để FE xử lý giao diện dễ hơn
        Map<String, Object> socketData = Map.of(
                "messageId", updatedMessage.getMessageId(),
                "content", updatedMessage.getContent(),
                "messageType", "recalled",
                "updatedAt", updatedMessage.getUpdatedAt().toString()
        );

        KafkaMessageEvent event = KafkaMessageEvent.builder()
                .eventType("MESSAGE_UPDATE") // Dùng UPDATE
                .conversationId(conversationId)
                .messageData(dto)
                .extraData(socketData)
                .build();

        kafkaTemplate.send(TOPIC_CHAT, String.valueOf(conversationId), event);

        return ApiResponse.success("Thu hồi tin nhắn thành công.", null);
    }

    //thả react cho 1 message

    public ApiResponse<String> reactToMessage(Integer conversationId, ReactionRequest request, String username) {
        if (request.getMessageId() == null || request.getReactionType() == null) {
            return ApiResponse.fail("Thiếu messageId hoặc type trong request.");
        }

        // 6 loại cảm xúc
        List<Integer> allowedTypes = List.of(1, 2, 3, 4, 5, 6);
        if (!allowedTypes.contains(request.getReactionType())) {
            return ApiResponse.fail("Type không hợp lệ. Chỉ được phép từ 1 đến 6.");
        }

        Optional<Message> optionalMessage = messageRepository.findById(request.getMessageId());
        if (optionalMessage.isEmpty()) {
            return ApiResponse.fail("Không tìm thấy tin nhắn.");
        }
        Message message = optionalMessage.get();

        if (!message.getConversation().getConversationId().equals(conversationId)) {
            return ApiResponse.fail("Tin nhắn không thuộc cuộc trò chuyện này.");
        }

        Account user = accountService.getAccountEntityByUsername(username);
        if (user == null) {
            return ApiResponse.fail("Không tìm thấy người dùng.");
        }

        Optional<MessageReaction> existingReaction =
                messageReactionRepository.findByMessage_MessageIdAndAccount_AccountId(request.getMessageId(), user.getAccountId());

        String action;
        if (existingReaction.isPresent()) {
            MessageReaction reaction = existingReaction.get();
            // Nếu click lại cùng type thì xóa reaction
            if (reaction.getReactionType().equals(request.getReactionType())) {
                messageReactionRepository.delete(reaction);
                action = "remove";
            } else {
                // Nếu khác type → cập nhật type mới
                reaction.setReactionType(String.valueOf(request.getReactionType()));
                reaction.setCreatedAt(LocalDateTime.now());
                messageReactionRepository.save(reaction);
                action = "update";
            }
        } else {
            // Tạo mới
            MessageReaction.MessageReactionId reactionId =
                    new MessageReaction.MessageReactionId(message.getMessageId(), user.getAccountId());
            MessageReaction reaction = MessageReaction.builder()
                    .id(reactionId)
                    .message(message)
                    .account(user)
                    .reactionType(String.valueOf(request.getReactionType()))
                    .createdAt(LocalDateTime.now())
                    .build();
            messageReactionRepository.save(reaction);
            action = "add";
        }

        Map<String, Object> socketData = Map.of(
                "messageId", request.getMessageId(),
                "accountId", user.getAccountId(),
                "reactionType", request.getReactionType(),
                "action", action
        );

        KafkaMessageEvent event = KafkaMessageEvent.builder()
                .eventType("MESSAGE_REACT")
                .conversationId(conversationId)
                .extraData(socketData) // Dữ liệu reaction
                .build();

        kafkaTemplate.send(TOPIC_CHAT, String.valueOf(conversationId), event);

        return ApiResponse.success("Reaction xử lý thành công (" + action + ").", action);

    }

    // Tải các tệp và ảnh lên đoạn chat
    public ApiResponse<MessageResponseDTO> uploadAttachment(Integer conversationId, MultipartFile file, String username) {
        try {
            // Kiểm tra cuộc trò chuyện hợp lệ
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy cuộc trò chuyện."));

            Account sender = accountService.getAccountEntityByUsername(username);
            if (sender == null) {
                return ApiResponse.fail("Không tìm thấy người gửi.");
            }

            // Lưu file vật lý
            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            File directory = new File(uploadDir);
            if (!directory.exists()) directory.mkdirs();

            String originalName = file.getOriginalFilename();
            String storedName = System.currentTimeMillis() + "_" + originalName;
            String filePath = uploadDir + storedName;

            file.transferTo(new File(filePath));

            // Tạo message mới
            Message message = Message.builder()
                    .conversation(conversation)
                    .sender(sender)
                    .content(originalName)
                    .messageType("file")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            messageRepository.save(message);

            // Tạo entity Attachment gắn với message
            Attachment attachment = Attachment.builder()
                    .message(message)
                    .fileUrl("/uploads/" + storedName)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .uploadedAt(LocalDateTime.now())
                    .build();
            attachmentRepository.save(attachment);

            // DTO phản hồi
            MessageResponseDTO dto = MessageResponseDTO.builder()
                    .messageId(message.getMessageId())
                    .conversationId(conversationId)
                    .senderId(sender.getAccountId())
                    .content(originalName)
                    .messageType("file")
                    .createdAt(message.getCreatedAt().toString())
                    .build();

            //Gửi realtime JSON
            Map<String, Object> socketData = Map.of(
                    "messageId", message.getMessageId(),
                    "sender", sender.getDisplayName(),
                    "fileName", originalName,
                    "fileUrl", "/uploads/" + storedName,
                    "fileType", file.getContentType(),
                    "fileSize", file.getSize(),
                    "createdAt", message.getCreatedAt().toString()
            );

            KafkaMessageEvent event = KafkaMessageEvent.builder()
                    .eventType("FILE_SENT")
                    .conversationId(conversationId)
                    .messageData(dto)
                    .extraData(socketData)
                    .build();

            kafkaTemplate.send(TOPIC_CHAT, String.valueOf(conversationId), event);

            return ApiResponse.success("Gửi file thành công.", dto);

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail("Lỗi khi tải file: " + e.getMessage());
        }
    }

    @Override
    public ApiResponse<List<MessageDocument>> searchMessages(String query, String username) {
        return null;
    }


    // Tìm kiếm tin nhắn bằng elasticsearch
//    @Override
//    public ApiResponse<List<MessageDocument>> searchMessages(String query, String username) {
//        // Lấy thông tin người dùng hiện tại
//        Account currentUser = accountService.getAccountEntityByUsername(username);
//        if (currentUser == null) {
//            return ApiResponse.fail("Không tìm thấy người dùng.");
//        }
//        Integer currentUserId = currentUser.getAccountId();
//
//        // Tạo một Set để chứa tất cả ID cuộc trò chuyện
//        Set<Integer> allConversationIds = new HashSet<>();
//
//        // LẤY ID CỦA CÁC NHÓM CHAT
//        List<Integer> groupIds = participationRepository
//                .findAllByAccount_AccountId(currentUserId)
//                .stream()
//                .map(participant -> participant.getConversation().getConversationId())
//                .collect(Collectors.toList());
//        allConversationIds.addAll(groupIds);
//
//        // Lấy danh sách bạn bè đã accept
//        List<UserContact> contacts = userContactRepository.findAllAcceptedByAccountId(currentUserId);
//
//        for (UserContact contact : contacts) {
//            // Với mỗi người bạn, tìm xem đã có cuộc trò chuyện private chưa
//            Conversation privateConv = conversationRepository.findPrivateConversationBetween(
//                    currentUserId,
//                    contact.getContactUser().getAccountId()
//            );
//
//            if (privateConv != null) {
//                allConversationIds.add(privateConv.getConversationId());
//            }
//        }
//
//        //Nếu không thì trả về rỗng
//        if (allConversationIds.isEmpty()) {
//            return ApiResponse.success("Không tìm thấy kết quả.", List.of());
//        }
//        // Tìm kiếm trong Elasticsearch với danh sách ID tổng hợp
//        List<MessageDocument> results = messageSearchRepository
//                .findByContentContainingAndConversationIdInAndMessageTypeNot(
//                        query,
//                        new ArrayList<>(allConversationIds),
//                        String.valueOf(Message_Status.recalled)
//                );
//        return ApiResponse.success("Tìm thấy " + results.size() + " kết quả.", results);
//    }
// Tìm kiếm tin nhắn trong một cuộc trò chuyện cụ thể bằng Elasticsearch

    @Override
    public ApiResponse<List<MessageDocument>> searchMessages(Integer conversationId, String query, String username) {
        // Lấy thông tin người dùng hiện tại
        Account currentUser = accountService.getAccountEntityByUsername(username);
        if (currentUser == null) {
            return ApiResponse.fail("Không tìm thấy người dùng.");
        }
        Integer currentUserId = currentUser.getAccountId();

        // Kiểm tra xem người dùng có tham gia cuộc trò chuyện này không
        boolean isParticipant = participationRepository.existsByConversation_ConversationIdAndAccount_AccountId(conversationId, currentUserId);

        // Nếu không có trong bảng Participation, kiểm tra xem có phải là cuộc trò chuyện Private không
        if (!isParticipant) {
            // Tìm cuộc trò chuyện để kiểm tra xem nó có tồn tại không
            Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
            if (conversation == null) {
                return ApiResponse.fail("Không tìm thấy cuộc trò chuyện.");
            }

            // không lưu đủ record cho cả 2 người trong chat đơn.
        }

        // 3. Tìm kiếm trong Elasticsearch chỉ với ID của cuộc trò chuyện này
        List<MessageDocument> results = messageSearchRepository
                .findByContentContainingAndConversationIdInAndMessageTypeNot(
                        query,
                        List.of(conversationId),
                        "recalled" //các tin nhắn đã thu hồi
                );

        return ApiResponse.success("Tìm thấy " + results.size() + " kết quả trong cuộc trò chuyện này.", results);
    }
    // Đánh dấu  các tin nhắn đã đọc
    public ApiResponse<Void> markAsRead(Integer conversationId, String username) {
        Account user = accountService.getAccountEntityByUsername(username);
        if (user == null) return ApiResponse.fail("User not found");

        // Update DB
        List<MessageStatus> unreadStatuses = messageStatusRepository.findUnreadStatuses(user.getAccountId(), conversationId);
        if (!unreadStatuses.isEmpty()) {
            for (MessageStatus status : unreadStatuses) {
                status.setStatus(Message_Status.read);
                status.setUpdatedAt(LocalDateTime.now());
            }
            messageStatusRepository.saveAll(unreadStatuses);

            // (Optional) Gửi socket báo đối phương đã xem...
        }

        //  Reset biến đếm thông báo trong Redis
        redisService.resetUnreadNotification(user.getAccountId());

        return ApiResponse.success("Đã đánh dấu đã đọc", null);
    }
    
    // Lấy danh sách tin nhắn của 1 hội thoại
    @Override
    public ApiResponse<org.springframework.data.domain.Page<MessageResponseDTO>> getMessagesByConversation(Integer conversationId, int page, int size, String username) {
        // Kiểm tra xem user có tồn tại không
        Account user = accountService.getAccountEntityByUsername(username);
        if (user == null) {
            return ApiResponse.fail("Không tìm thấy người dùng.");
        }

        // Kiểm tra sự tồn tại của cuộc trò chuyện
        Optional<Conversation> optionalConversation = conversationRepository.findById(conversationId);
        if (optionalConversation.isEmpty()) {
            return ApiResponse.fail("Không tìm thấy cuộc trò chuyện.");
        }

        // Kiểm tra user có nằm trong cuộc trò chuyện này không
        Optional<Participants> participantOpt = participationRepository.findParticipant(conversationId, user.getAccountId());
        if (participantOpt.isEmpty()) {
            return ApiResponse.fail("Bạn không có quyền truy cập vào chức năng của cuộc trò chuyện này.");
        }

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        // Lấy danh sách tin nhắn theo ConversationId, xếp mới nhất lên đầu
        org.springframework.data.domain.Page<Message> messagePage = messageRepository.findByConversation_ConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        org.springframework.data.domain.Page<MessageResponseDTO> messageDTOPage = messagePage.map(this::toDTO);
        return ApiResponse.success("Lấy danh sách tin nhắn thành công.", messageDTOPage);
    }
}
