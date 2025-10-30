package com.bkav.webchat.service;

import com.bkav.webchat.dto.ApiResponse;
import com.bkav.webchat.dto.m.MessageResponseDTO;
import com.bkav.webchat.dto.request.ChatMessageRequest;

public interface MessageService {
    ApiResponse sendMessage(Integer conversationId,ChatMessageRequest request);
    ApiResponse<MessageResponseDTO> updateMessage(Integer conversationId, ChatMessageRequest request);
    ApiResponse<Void> deleteMessage(Integer conversationId, ChatMessageRequest request);

}
