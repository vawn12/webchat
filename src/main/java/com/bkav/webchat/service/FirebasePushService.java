package com.bkav.webchat.service;

public interface FirebasePushService {
    void notifyChatBump(Integer userId, Long conversationId, String senderName, String content);
    boolean sendTestNotification(Integer userId, String title, String body);
}
