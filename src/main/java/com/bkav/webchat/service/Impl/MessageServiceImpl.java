package com.bkav.webchat.service.Impl;

import com.bkav.webchat.dto.ApiResponse;
import com.bkav.webchat.dto.m.MessageResponseDTO;
import com.bkav.webchat.dto.request.ChatMessageRequest;
import com.bkav.webchat.entity.Account;
import com.bkav.webchat.entity.Conversation;
import com.bkav.webchat.entity.Message;
import com.bkav.webchat.entity.MessageStatus;
import com.bkav.webchat.repository.ConversationRepository;
import com.bkav.webchat.repository.MessageRepository;
import com.bkav.webchat.repository.MessageStatusRepository;
import com.bkav.webchat.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    private Message toEntity(ChatMessageRequest request, Conversation conversation, Account sender) {
        return Message.builder()
                .ConversationId(conversation)
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
                .conversationId(message.getConversationId().getConversationId())
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
    public ApiResponse sendMessage(Integer conversationId,ChatMessageRequest request) {
        //  Kiểm tra tồn tại cuộc trò chuyện
        Optional<Conversation> optionalConversation = conversationRepository.findById(conversationId);
        if (optionalConversation.isEmpty()) {
            return ApiResponse.success( "Không tìm thấy cuộc trò chuyện.", null);
        }

        // Giả lập người gửi
        Account sender = new Account();
        sender.setAccountId(1);

        // Tạo và lưu Message Entity
        Message message = toEntity(request, optionalConversation.get(), sender);
        message = messageRepository.save(message);

        // Lưu MessageStatus
        MessageStatus status = toMessageStatus(message, sender, "sent");
        messageStatusRepository.save(status);
        MessageResponseDTO dto = toDTO(message);

        // Gửi real-time message qua WebSocket topic
        messagingTemplate.convertAndSend("/topic/conversation/" + dto.getConversationId(), dto);
        return ApiResponse.success( "Gửi tin nhắn thành công.", dto);
    }
    //update
    public ApiResponse<MessageResponseDTO> updateMessage(Integer conversationId, ChatMessageRequest request) {
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
        if (!message.getConversationId().getConversationId().equals(conversationId.intValue())) {
            return ApiResponse.fail("Tin nhắn không thuộc cuộc trò chuyện này.");
        }

        message.setContent(request.getContent());
        message.setMessageType(request.getMessageType());
        message.setUpdatedAt(LocalDateTime.now());
        messageRepository.save(message);

        MessageResponseDTO dto = toDTO(message);
        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, dto);

        return ApiResponse.success("Cập nhật tin nhắn thành công.", dto);
    }

    // ------------------ XÓA TIN NHẮN ------------------
    public ApiResponse<Void> deleteMessage(Integer conversationId, ChatMessageRequest request) {
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
        if (!message.getConversationId().getConversationId().equals(conversationId.intValue())) {
            return ApiResponse.fail("Tin nhắn không thuộc cuộc trò chuyện này.");
        }

        messageRepository.delete(message);
        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, "deleted:" + request.getMessageId());

        return ApiResponse.success("Xóa tin nhắn thành công.", null);
    }

}
