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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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

        Account sender = accountService.getAccountEntityByUsername(username);
        if (sender == null) {
            return ApiResponse.fail("Không tìm thấy người gửi.");
        }

        Message message = createAndSaveMessage(request, optionalConversation.get(), sender, conversationId);
        saveInitialMessageStatuses(message, sender, request.getReceiverId());

        MessageResponseDTO dto = toDTO(message);
        sendKafkaChatEvent("MESSAGE_SENT", conversationId, dto, null);

        return ApiResponse.success("Gửi tin nhắn thành công.", dto);
    }

    private Message createAndSaveMessage(ChatMessageRequest request, Conversation conversation, Account sender, Integer conversationId) {
        Message message = toEntity(request, conversation, sender);
        processReplyMetadata(message, request, conversationId);
        return messageRepository.save(message);
    }

    private void processReplyMetadata(Message message, ChatMessageRequest request, Integer conversationId) {
        if (request.getRepliedToMessageId() == null) {
            return;
        }

        messageRepository.findById(request.getRepliedToMessageId())
                .filter(original -> original.getConversation().getConversationId().equals(conversationId))
                .ifPresent(original -> {
                    Map<String, Object> replyInfo = Map.of(
                            "repliedToMessageId", original.getMessageId(),
                            "repliedToSenderId", original.getSender().getAccountId(),
                            "repliedToSenderName", original.getSender().getDisplayName(),
                            "repliedToContent", original.getContent()
                    );
                    try {
                        message.setMetadata(objectMapper.writeValueAsString(Map.of("replyInfo", replyInfo)));
                    } catch (JsonProcessingException e) {
                        log.error("Error serializing reply metadata", e);
                    }
                });
    }

    private void saveInitialMessageStatuses(Message message, Account sender, Integer receiverId) {
        // Lưu trạng thái cho người gửi
        messageStatusRepository.save(toMessageStatus(message, sender, Message_Status.sent));

        // Lưu trạng thái cho người nhận (nếu có)
        if (receiverId != null) {
            Account receiver = accountService.getAccountById(receiverId);
            if (receiver != null) {
                messageStatusRepository.save(toMessageStatus(message, receiver, Message_Status.delivered));
            }
        }
    }

    private void sendKafkaChatEvent(String eventType, Integer conversationId, MessageResponseDTO dto, Map<String, Object> extraData) {
        KafkaMessageEvent event = KafkaMessageEvent.builder()
                .eventType(eventType)
                .conversationId(conversationId)
                .messageData(dto)
                .extraData(extraData)
                .build();
        kafkaTemplate.send(TOPIC_CHAT, String.valueOf(conversationId), event);
    }
    //update tin nhắn của nguời gửi
    @Override
    @Transactional
    public ApiResponse<MessageResponseDTO> updateMessage(Integer conversationId, ChatMessageRequest request, String username) {
        ApiResponse<Message> validation = validateMessageOwnership(conversationId, request.getMessageId(), username, "cập nhật");
        if (!validation.isSuccess()) {
            return ApiResponse.fail(validation.getMessage());
        }

        Message message = validation.getData();
        updateMessageContent(message, request);
        Message updated = messageRepository.save(message);

        MessageResponseDTO dto = toDTO(updated);
        sendKafkaChatEvent("MESSAGE_UPDATE", conversationId, dto, getUpdateSocketData(updated));

        return ApiResponse.success("Cập nhật thành công.", dto);
    }

    private void updateMessageContent(Message message, ChatMessageRequest request) {
        message.setContent(request.getContent());
        message.setMessageType(request.getMessageType());
        message.setUpdatedAt(LocalDateTime.now());
    }

    private Map<String, Object> getUpdateSocketData(Message message) {
        return Map.of(
                "messageId", message.getMessageId(),
                "senderId", message.getSender().getAccountId(),
                "content", message.getContent(),
                "messageType", message.getMessageType(),
                "updatedAt", message.getUpdatedAt().toString()
        );
    }

    // XÓA TIN NHẮN
    // Chỉ có thể xóa tin nhắn của mình gửi và không thể xóa tin nhắn của đối phương
    @Override
    @Transactional
    public ApiResponse<Void> deleteMessage(Integer conversationId, ChatMessageRequest request, String username) {
        ApiResponse<Message> validation = validateMessageOwnership(conversationId, request.getMessageId(), username, "xóa");
        if (!validation.isSuccess()) {
            return ApiResponse.fail(validation.getMessage());
        }

        Message message = validation.getData();
        deleteMessageAttachments(message);
        recallMessage(message);

        Message recalledMessage = messageRepository.save(message);
        updateMessageStatusesToRecalled(recalledMessage);

        sendKafkaChatEvent("MESSAGE_UPDATE", conversationId, toDTO(recalledMessage), getRecallSocketData(recalledMessage));

        return ApiResponse.success("Thu hồi tin nhắn thành công.", null);
    }

    private void deleteMessageAttachments(Message message) {
        List<Attachment> attachments = attachmentRepository.findByMessage(message);
        if (attachments != null && !attachments.isEmpty()) {
            attachmentRepository.deleteAll(attachments);
        }
    }

    private void recallMessage(Message message) {
        message.setContent("Tin nhắn đã được thu hồi");
        message.setMessageType("recalled");
        message.setUpdatedAt(LocalDateTime.now());
        message.setMetadata(null);
    }

    private void updateMessageStatusesToRecalled(Message message) {
        List<MessageStatus> statuses = messageStatusRepository.findAllByMessage(message);
        for (MessageStatus status : statuses) {
            status.setStatus(Message_Status.recalled);
            status.setUpdatedAt(LocalDateTime.now());
        }
        messageStatusRepository.saveAll(statuses);
    }

    private Map<String, Object> getRecallSocketData(Message message) {
        return Map.of(
                "messageId", message.getMessageId(),
                "content", message.getContent(),
                "messageType", "recalled",
                "updatedAt", message.getUpdatedAt().toString()
        );
    }

    private ApiResponse<Message> validateMessageOwnership(Integer conversationId, Integer messageId, String username, String actionName) {
        if (messageId == null) {
            return ApiResponse.fail("Thiếu messageId trong request.");
        }

        Message message = messageRepository.findById(messageId).orElse(null);
        if (message == null) {
            return ApiResponse.fail("Không tìm thấy tin nhắn cần " + actionName + ".");
        }

        if (!message.getConversation().getConversationId().equals(conversationId)) {
            return ApiResponse.fail("Tin nhắn không thuộc cuộc trò chuyện này.");
        }

        Account currentUser = accountService.getAccountEntityByUsername(username);
        if (currentUser == null || !message.getSender().getAccountId().equals(currentUser.getAccountId())) {
            return ApiResponse.fail("Bạn không có quyền " + actionName + " tin nhắn của người khác.");
        }

        return ApiResponse.success("Success", message);
    }

    //thả react cho 1 message

    @Override
    @Transactional
    public ApiResponse<String> reactToMessage(Integer conversationId, ReactionRequest request, String username) {
        ApiResponse<Void> validation = validateReactionRequest(conversationId, request, username);
        if (!validation.isSuccess()) {
            return ApiResponse.fail(validation.getMessage());
        }

        Account user = accountService.getAccountEntityByUsername(username);
        String action = handleReactionLogic(request, user);

        sendKafkaChatEvent("MESSAGE_REACT", conversationId, null, getReactionSocketData(request.getMessageId(), user.getAccountId(), request.getReactionType(), action));

        return ApiResponse.success("Reaction xử lý thành công (" + action + ").", action);
    }

    private ApiResponse<Void> validateReactionRequest(Integer conversationId, ReactionRequest request, String username) {
        if (request.getMessageId() == null || request.getReactionType() == null) {
            return ApiResponse.fail("Thiếu messageId hoặc type trong request.");
        }

        List<Integer> allowedTypes = List.of(1, 2, 3, 4, 5, 6);
        if (!allowedTypes.contains(request.getReactionType())) {
            return ApiResponse.fail("Type không hợp lệ. Chỉ được phép từ 1 đến 6.");
        }

        Message message = messageRepository.findById(request.getMessageId()).orElse(null);
        if (message == null || !message.getConversation().getConversationId().equals(conversationId)) {
            return ApiResponse.fail("Tin nhắn không hợp lệ.");
        }

        if (accountService.getAccountEntityByUsername(username) == null) {
            return ApiResponse.fail("Không tìm thấy người dùng.");
        }

        return ApiResponse.success("Valid", null);
    }

    private String handleReactionLogic(ReactionRequest request, Account user) {
        Optional<MessageReaction> existing = messageReactionRepository.findByMessage_MessageIdAndAccount_AccountId(request.getMessageId(), user.getAccountId());

        if (existing.isPresent()) {
            MessageReaction reaction = existing.get();
            if (reaction.getReactionType().equals(String.valueOf(request.getReactionType()))) {
                messageReactionRepository.delete(reaction);
                return "remove";
            }
            reaction.setReactionType(String.valueOf(request.getReactionType()));
            reaction.setCreatedAt(LocalDateTime.now());
            messageReactionRepository.save(reaction);
            return "update";
        }

        Message message = messageRepository.getReferenceById(request.getMessageId());
        MessageReaction newReaction = MessageReaction.builder()
                .id(new MessageReaction.MessageReactionId(message.getMessageId(), user.getAccountId()))
                .message(message)
                .account(user)
                .reactionType(String.valueOf(request.getReactionType()))
                .createdAt(LocalDateTime.now())
                .build();
        messageReactionRepository.save(newReaction);
        return "add";
    }

    private Map<String, Object> getReactionSocketData(Integer messageId, Integer accountId, Integer type, String action) {
        return Map.of(
                "messageId", messageId,
                "accountId", accountId,
                "reactionType", type,
                "action", action
        );
    }

    // Tải các tệp và ảnh lên đoạn chat
    @Override
    @Transactional
    public ApiResponse<MessageResponseDTO> uploadAttachment(Integer conversationId, MultipartFile file, String username) {
        try {
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy cuộc trò chuyện."));

            Account sender = accountService.getAccountEntityByUsername(username);
            if (sender == null) return ApiResponse.fail("Không tìm thấy người gửi.");

            String storedName = savePhysicalFile(file);
            Message message = saveAttachmentMessage(conversation, sender, file.getOriginalFilename());
            saveAttachmentEntity(message, file, storedName);

            MessageResponseDTO dto = toDTO(message);
            sendKafkaChatEvent("FILE_SENT", conversationId, dto, getFileSocketData(message, sender.getDisplayName(), file, storedName));

            return ApiResponse.success("Gửi file thành công.", dto);
        } catch (Exception e) {
            log.error("Lỗi khi tải file", e);
            return ApiResponse.fail("Lỗi khi tải file: " + e.getMessage());
        }
    }

    private String savePhysicalFile(MultipartFile file) throws Exception {
        String uploadDir = System.getProperty("user.dir") + "/uploads/";
        File directory = new File(uploadDir);
        if (!directory.exists()) directory.mkdirs();

        String storedName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        file.transferTo(new File(uploadDir + storedName));
        return storedName;
    }

    private Message saveAttachmentMessage(Conversation conversation, Account sender, String fileName) {
        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(fileName)
                .messageType("file")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return messageRepository.save(message);
    }

    private void saveAttachmentEntity(Message message, MultipartFile file, String storedName) {
        Attachment attachment = Attachment.builder()
                .message(message)
                .fileUrl("/uploads/" + storedName)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .build();
        attachmentRepository.save(attachment);
    }

    private Map<String, Object> getFileSocketData(Message message, String senderDisplayName, MultipartFile file, String storedName) {
        return Map.of(
                "messageId", message.getMessageId(),
                "sender", senderDisplayName,
                "fileName", message.getContent(),
                "fileUrl", "/uploads/" + storedName,
                "fileType", file.getContentType(),
                "fileSize", file.getSize(),
                "createdAt", message.getCreatedAt().toString()
        );
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

        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        // Lấy danh sách tin nhắn theo ConversationId, xếp mới nhất lên đầu
        Page<Message> messagePage = messageRepository.findByConversation_ConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        Page<MessageResponseDTO> messageDTOPage = messagePage.map(this::toDTO);
        return ApiResponse.success("Lấy danh sách tin nhắn thành công.", messageDTOPage);
    }
}
