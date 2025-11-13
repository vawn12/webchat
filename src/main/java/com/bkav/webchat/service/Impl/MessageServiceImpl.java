package com.bkav.webchat.service.Impl;

import com.bkav.webchat.dto.ApiResponse;
import com.bkav.webchat.dto.MessageResponseDTO;
import com.bkav.webchat.dto.request.ChatMessageRequest;
import com.bkav.webchat.dto.request.ReactionRequest;
import com.bkav.webchat.entity.*;
import com.bkav.webchat.repository.*;
import com.bkav.webchat.service.AccountService;
import com.bkav.webchat.service.MessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service

public class MessageServiceImpl implements MessageService {
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
    private ParticipationRepository participationRepository;
    @Autowired
    private MessageSearchRepository messageSearchRepository;
    @Autowired
    private ObjectMapper objectMapper;


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
    private MessageStatus toMessageStatus(Message message, Account sender, String statusText) {
        return MessageStatus.builder()
                .message(message)
                .account(sender)
                .status(statusText)
                .updatedAt(LocalDateTime.now())
                .build();
    }
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
            return ApiResponse.fail("Không tìm thấy người gửi (user).");
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
        MessageStatus status = toMessageStatus(message, sender, "sent");
        messageStatusRepository.save(status);
        //lưu

        eventPublisher.publishEvent(new MessageSyncEvent(message, "SAVE"));

        MessageResponseDTO dto = toDTO(message);

        // Gửi real-time message qua WebSocket topic
        var payload = Map.of(
                "event", "MESSAGE_SENT",
                "conversationId", conversationId,
                "data", dto
        );
        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, payload);

        return ApiResponse.success("Gửi tin nhắn thành công.", dto);
    }
    //update
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

        eventPublisher.publishEvent(new MessageSyncEvent(updated, "SAVE"));

        MessageResponseDTO dto = toDTO(updated);
        var payload = Map.of(
                "event", "MESSAGE_UPDATE",
                "conversationId", conversationId,
                "data", Map.of(
                        "messageId", updated.getMessageId(),
                        "senderId", updated.getSender().getAccountId(),
                        "content", updated.getContent(),
                        "messageType", updated.getMessageType(),
                        "updatedAt", updated.getUpdatedAt().toString()
                )
        );

        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, payload);

        return ApiResponse.success("Cập nhật tin nhắn thành công.", dto);
    }

    // XÓA TIN NHẮN
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

        messageRepository.delete(message);
        eventPublisher.publishEvent(new MessageSyncEvent(message, "DELETE"));

        var payload = Map.of(
                "event", "MESSAGE_DELETE",
                "conversationId", conversationId,
                "data", Map.of("messageId", request.getMessageId())
        );
        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, payload);

        return ApiResponse.success("Xóa tin nhắn thành công.", null);
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

        var payload = Map.of(
                "event", "MESSAGE_REACT",
                "conversationId", conversationId,
                "data", Map.of(
                        "messageId", request.getMessageId(),
                        "accountId", user.getAccountId(),
                        "reactionType", request.getReactionType(),
                        "action", action
                )
        );
        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, payload);
        return ApiResponse.success("Reaction xử lý thành công (" + action + ").", action);

    }


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
            var payload = Map.of(
                    "event", "FILE_SENT",
                    "conversationId", conversationId,
                    "data", Map.of(
                            "messageId", message.getMessageId(),
                            "sender", sender.getDisplayName(),
                            "fileName", originalName,
                            "fileUrl", "/uploads/" + storedName,
                            "fileType", file.getContentType(),
                            "fileSize", file.getSize(),
                            "createdAt", message.getCreatedAt().toString()
                    )
            );

            messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, payload);

            return ApiResponse.success("Gửi file thành công.", dto);

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail("Lỗi khi tải file: " + e.getMessage());
        }
    }


    public ApiResponse<List<MessageDocument>> searchMessages(String query, String username) {

        // Lấy thông tin người dùng
        Account currentUser = accountService.getAccountEntityByUsername(username);
        if (currentUser == null) {
            return ApiResponse.fail("Không tìm thấy người dùng.");
        }

        // Lấy danh sách các cuộc trò chuyện user được phép xem
        List<Integer> userConversationIds = participationRepository
                .findAllByAccount_AccountId(currentUser.getAccountId())
                .stream()
                .map(participant -> participant.getConversation().getConversationId())
                .collect(Collectors.toList());

        if (userConversationIds.isEmpty()) {
            return ApiResponse.success("Không tìm thấy kết quả.", List.of());
        }

        List<MessageDocument> results =
                messageSearchRepository.findByContentContainingAndConversationIdIn(query, userConversationIds);


        return ApiResponse.success("Tìm thấy " + results.size() + " kết quả.", results);
    }
}
