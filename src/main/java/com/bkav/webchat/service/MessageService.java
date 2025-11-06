package com.bkav.webchat.service;

import com.bkav.webchat.dto.ApiResponse;
import com.bkav.webchat.dto.m.MessageResponseDTO;
import com.bkav.webchat.dto.request.ChatMessageRequest;
import com.bkav.webchat.dto.request.ReactionRequest;
import org.springframework.web.multipart.MultipartFile;

public interface MessageService {
    ApiResponse<MessageResponseDTO> sendMessage(Integer conversationId, ChatMessageRequest request, String username);
    ApiResponse<MessageResponseDTO> updateMessage(Integer conversationId, ChatMessageRequest request,String username);
    ApiResponse<Void> deleteMessage(Integer conversationId, ChatMessageRequest request,String username);
    ApiResponse<String> reactToMessage(Integer conversationId, ReactionRequest request, String username);
    ApiResponse<String> uploadAttachment(Integer conversationId, MultipartFile file, Integer messageId, String username);
}
