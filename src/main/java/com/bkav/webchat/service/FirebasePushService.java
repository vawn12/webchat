package com.bkav.webchat.service;

import java.util.List;

public interface FirebasePushService {
    void notifyChatBump(Integer userId, Long conversationId, String senderName, String content);
    boolean sendTestNotification(Integer userId, String title, String body);
    void sendNotificationToGroup(List<Integer> recipientIds, Long conversationId, String title, String body);
}
