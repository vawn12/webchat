package com.bkav.webchat.service.Impl;

import com.bkav.webchat.entity.UserDeviceToken;
import com.bkav.webchat.repository.UserDeviceTokenRepository;
import com.bkav.webchat.service.FirebasePushService;
import com.google.firebase.messaging.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FirebasePushServiceImp implements FirebasePushService {

    @Autowired
    private UserDeviceTokenRepository tokenRepository;
    // Gửi thông báo đến các thiết bị được sử dụng
    @Override
    public void notifyChatBump(Integer userId, Long conversationId, String senderName, String content) {
        sendNotificationToGroup(List.of(userId), conversationId, senderName, content);
    }
    @Override
    @Transactional
    public void sendNotificationToGroup(List<Integer> recipientIds, Long conversationId, String title, String body) {
        if (recipientIds == null || recipientIds.isEmpty()) return;

        // Lấy tất cả token chỉ với 1 lệnh SELECT
        List<UserDeviceToken> deviceTokens = tokenRepository.findAllByAccount_AccountIdIn(recipientIds);

        if (deviceTokens.isEmpty()) return;

        // Lấy list String token và loại bỏ trùng lặp
        List<String> fcmTokens = deviceTokens.stream()
                .map(UserDeviceToken::getToken)
                .distinct()
                .collect(Collectors.toList());

        if (fcmTokens.isEmpty()) return;

        // Firebase chỉ cho gửi tối đa 500 token/lần
        // Nếu nhóm có 1000 thiết bị -> Chia làm 2 lần gửi
        int batchSize = 500;
        for (int i = 0; i < fcmTokens.size(); i += batchSize) {
            int end = Math.min(i + batchSize, fcmTokens.size());
            List<String> subListTokens = fcmTokens.subList(i, end);

            sendBatchToFirebase(subListTokens, conversationId, title, body);
        }
    }
    // Hàm phụ để gửi một batch (tối đa 500)
    private void sendBatchToFirebase(List<String> tokens, Long conversationId, String title, String body) {
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .putData("type", "NEW_MESSAGE")
                .putData("conversationId", conversationId.toString())
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body.length() > 100 ? body.substring(0, 100) + "..." : body)
                        .build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setClickAction("OPEN_CHAT_ACTIVITY")
                                .build())
                        .build())
                .setWebpushConfig(WebpushConfig.builder()
                        .setNotification(WebpushNotification.builder()
                                .setIcon("/logo.png")
                                .build())
                        .build())
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);

            // Xử lý token lỗi
            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                List<String> failedTokens = new ArrayList<>();
                for (int j = 0; j < responses.size(); j++) {
                    if (!responses.get(j).isSuccessful()) {
                        MessagingErrorCode errorCode = responses.get(j).getException().getMessagingErrorCode();
                        if (MessagingErrorCode.UNREGISTERED.equals(errorCode) ||
                                MessagingErrorCode.INVALID_ARGUMENT.equals(errorCode)) {
                            failedTokens.add(tokens.get(j));
                        }
                    }
                }
                if (!failedTokens.isEmpty()) {
                    removeInvalidTokens(failedTokens);
                }
            }
        } catch (FirebaseMessagingException e) {
            System.err.println("Lỗi gửi Firebase: " + e.getMessage());
        }
    }
    // Test gửi tin nhắn
    @Override
    @Transactional
    public boolean sendTestNotification(Integer userId, String title, String body) {
        // Kiểm tra xem user có token không TRƯỚC khi gửi
        List<UserDeviceToken> deviceTokens = tokenRepository.findByAccount_AccountId(userId);

        if (deviceTokens.isEmpty()) {
            System.out.println("User ID " + userId + " chưa đăng nhập thiết bị nào (Không có token).");
            return false; // Trả về false ngay lập tức
        }

        //  Nếu có token thì mới gọi hàm gửi
        try {
            // Gọi hàm gửi nhóm
            sendNotificationToGroup(List.of(userId), 0L, title, body);
            return true;
        } catch (Exception e) {
            //  In lỗi ra để debug trường hợp User có token mà vẫn fail
            System.err.println("Lỗi khi gửi Firebase: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void removeInvalidTokens(List<String> tokens) {
        for (String token : tokens) {
            tokenRepository.deleteByToken(token);
        }
    }
}
