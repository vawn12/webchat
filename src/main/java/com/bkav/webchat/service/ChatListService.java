package com.bkav.webchat.service;

import com.bkav.webchat.dto.ChatListDTO;

import java.util.List;

public interface ChatListService {
    List<ChatListDTO> getChatList(Long userId);
    void notifyChatUpdate(Long conversationId);
}
