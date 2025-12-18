package com.bkav.webchat.service;

import com.bkav.webchat.dto.m.ChatListDTO;

import java.util.List;

public interface ContactService {
    List<ChatListDTO> getChatList(Long userId);
    void notifyChatUpdate(Long conversationId);
}
