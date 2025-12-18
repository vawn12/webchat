package com.bkav.webchat.service;

import com.bkav.webchat.dto.response.ApiResponse;
import com.bkav.webchat.dto.response.MessageResponseDTO;
import com.bkav.webchat.dto.request.ChatMessageRequest;
import com.bkav.webchat.dto.request.ReactionRequest;
import com.bkav.webchat.entity.MessageDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MessageService {
    ApiResponse<MessageResponseDTO> sendMessage(Integer conversationId, ChatMessageRequest request, String username);
    ApiResponse<MessageResponseDTO> updateMessage(Integer conversationId, ChatMessageRequest request,String username);
    ApiResponse<Void> deleteMessage(Integer conversationId, ChatMessageRequest request,String username);
    ApiResponse<String> reactToMessage(Integer conversationId, ReactionRequest request, String username);
    ApiResponse<MessageResponseDTO> uploadAttachment(Integer conversationId, MultipartFile file, String username);
    ApiResponse<List<MessageDocument>> searchMessages(String query, String username);
    ApiResponse<Void> markAsRead(Integer conversationId, String username);
}
