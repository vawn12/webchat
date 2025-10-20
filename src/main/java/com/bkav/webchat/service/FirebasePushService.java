package com.bkav.webchat.service;

public interface FirebasePushService {
    void notifyChatBump(Integer userId, Long conversationId);
}
