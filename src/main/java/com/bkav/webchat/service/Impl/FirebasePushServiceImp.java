//package com.bkav.webchat.service.Impl;
//
//import com.bkav.webchat.service.FirebasePushService;
//import com.google.firebase.messaging.*;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//@Service
//public class FirebasePushServiceImp implements FirebasePushService {
//
//    public void notifyChatBump(Integer userId, Long conversationId) {
//        //repository lưu token thiết bị người dùng
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
