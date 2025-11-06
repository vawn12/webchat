package com.bkav.webchat.service.Impl;

import com.bkav.webchat.dto.ApiResponse;
import com.bkav.webchat.dto.MessageResponseDTO;
import com.bkav.webchat.dto.request.ChatMessageRequest;
import com.bkav.webchat.dto.request.ReactionRequest;
import com.bkav.webchat.entity.*;
import com.bkav.webchat.repository.*;
import com.bkav.webchat.service.AccountService;
import com.bkav.webchat.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    private Message toEntity(ChatMessageRequest request, Conversation conversation, Account sender) {
        return Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getContent())
                .messageType(request.getMessageType())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }


    private MessageResponseDTO toDTO(Message message) {
        return MessageResponseDTO.builder()
                .messageId(message.getMessageId())
                .conversationId(message.getConversation().getConversationId())
                .senderId(message.getSender().getAccountId())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt().toString())
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
        message = messageRepository.save(message);

        // Lưu MessageStatus
        MessageStatus status = toMessageStatus(message, sender, "sent");
        messageStatusRepository.save(status);

        MessageResponseDTO dto = toDTO(message);

        // Gửi real-time message qua WebSocket topic
        messagingTemplate.convertAndSend("/topic/conversation/" + dto.getConversationId(), dto);
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
        messageRepository.save(message);

        MessageResponseDTO dto = toDTO(message);
        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, dto);

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
        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, "deleted:" + request.getMessageId());

        return ApiResponse.success("Xóa tin nhắn thành công.", null);
    }

    //thả react cho 1 message

    public ApiResponse<String> reactToMessage(Integer conversationId, ReactionRequest request, String username) {
        // Validate input
        if (request.getMessageId() == null || request.getReactionType() == null) {
            return ApiResponse.fail("Thiếu messageId hoặc reactionType trong request.");
        }

        // Chỉ cho phép 5 emoji mặc định
        List<String> allowedReactions = List.of("👍", "❤️", "😂", "😢", "😡");
        if (!allowedReactions.contains(request.getReactionType())) {
            return ApiResponse.fail("Reaction không hợp lệ. Chỉ được phép: " + allowedReactions);
        }

        // Kiểm tra tồn tại conversation và message
        Optional<Message> optionalMessage = messageRepository.findById(request.getMessageId());
        if (optionalMessage.isEmpty()) {
            return ApiResponse.fail("Không tìm thấy tin nhắn.");
        }
        Message message = optionalMessage.get();

        if (!message.getConversation().getConversationId().equals(conversationId)) {
            return ApiResponse.fail("Tin nhắn không thuộc cuộc trò chuyện này.");
        }

        // Lấy user hiện tại
        Account user = accountService.getAccountEntityByUsername(username);
        if (user == null) {
            return ApiResponse.fail("Không tìm thấy người dùng.");
        }

        // Kiểm tra xem user đã thả react chưa
        Optional<MessageReaction> existingReaction =
                messageReactionRepository.findByMessage_MessageIdAndAccount_AccountId(request.getMessageId(), user.getAccountId());

        if (existingReaction.isPresent()) {
            MessageReaction reaction = existingReaction.get();
            reaction.setReactionType(request.getReactionType());
            reaction.setCreatedAt(LocalDateTime.now());
            messageReactionRepository.save(reaction);
        } else {
            MessageReaction.MessageReactionId reactionId =
                    new MessageReaction.MessageReactionId(message.getMessageId(), user.getAccountId());
            MessageReaction reaction = MessageReaction.builder()
                    .id(reactionId)
                    .message(message)
                    .account(user)
                    .reactionType(request.getReactionType())
                    .createdAt(LocalDateTime.now())
                    .build();

            messageReactionRepository.save(reaction);
        }

        // Gửi thông báo realtime đến FE
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId,
                username + " reacted '" + request.getReactionType() + "' to message " + request.getMessageId()
        );

        return ApiResponse.success("Đã thả reaction thành công.", request.getReactionType());
    }


    public ApiResponse<String> uploadAttachment(Integer conversationId, MultipartFile file, Integer messageId, String username) {
        try {
            // Kiểm tra tin nhắn hợp lệ
            Optional<Message> optionalMessage = messageRepository.findById(messageId);
            if (optionalMessage.isEmpty()) {
                return ApiResponse.fail("Không tìm thấy tin nhắn để đính kèm file.");
            }
            Message message = optionalMessage.get();

            if (!message.getConversation().getConversationId().equals(conversationId)) {
                return ApiResponse.fail("Tin nhắn không thuộc cuộc trò chuyện này.");
            }

            // Lưu file vật lý (tạm thời trong /uploads)
            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            File directory = new File(uploadDir);
            if (!directory.exists()) directory.mkdirs();

            String originalName = file.getOriginalFilename();
            String filePath = uploadDir + System.currentTimeMillis() + "_" + originalName;
            file.transferTo(new File(filePath));

            // Tạo entity Attachment
            Attachment attachment = Attachment.builder()
                    .message(message)
                    .fileUrl(filePath)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .uploadedAt(LocalDateTime.now())
                    .build();
            attachmentRepository.save(attachment);

            //  Gửi realtime thông báo cho client trong đoạn chat
            messagingTemplate.convertAndSend(
                    "/topic/conversation/" + conversationId,
                    username + " đã gửi tệp tin: " + originalName
            );

            return ApiResponse.success("Tải file thành công.", filePath);

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail("Lỗi khi tải file: " + e.getMessage());
        }
    }

}
