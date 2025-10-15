//package com.bkav.webchat.service;
//
//import com.google.firebase.messaging.*;
//import org.springframework.stereotype.Service;
//import java.util.List;
//
//import java.util.List;
//
//public class FirebasePushService {
//
//    public void notifyChatBump(Long userId, Long conversationId) {
//        // Giả sử bạn có repository lưu token thiết bị người dùng
//        List<String> tokens = List.of(); // lấy từ DB
//        for (String token : tokens) {
//            Message msg = Message.builder()
//                    .setToken(token)
//                    .putData("type", "chat_update")
//                    .putData("conversationId", conversationId.toString())
//                    .setNotification(Notification.builder()
//                            .setTitle("Tin nhắn mới 💬")
//                            .setBody("Bạn có tin nhắn mới trong cuộc trò chuyện")
//                            .build())
//                    .build();
//            try {
//                FirebaseMessaging.getInstance().send(msg);
//            } catch (Exception ignored) {}
//        }
//    }
//}
