package com.bkav.webchat.service.Impl;

import com.bkav.webchat.entity.UserDeviceToken;
import com.bkav.webchat.repository.UserDeviceTokenRepository;
import com.bkav.webchat.service.FirebasePushService;
import com.google.firebase.messaging.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FirebasePushServiceImp implements FirebasePushService {

    @Autowired
    private UserDeviceTokenRepository tokenRepository;

    @Override
    public void notifyChatBump(Integer userId, Long conversationId, String senderName, String content) {
        // Lấy danh sách token của User nhận tin nhắn
        List<UserDeviceToken> deviceTokens = tokenRepository.findByAccount_AccountId(userId);

        if (deviceTokens.isEmpty()) return;

        List<String> tokens = deviceTokens.stream()
                .map(UserDeviceToken::getToken)
                .collect(Collectors.toList());

        // Tạo MulticastMessage gửi 1 phát cho nhiều thiết bị
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .putData("type", "NEW_MESSAGE")
                .putData("conversationId", conversationId.toString())
                .setNotification(Notification.builder()
                        .setTitle(senderName)
                        .setBody(content.length() > 50 ? content.substring(0, 50) + "..." : content)
                        .build())
                // Config cho Web
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setClickAction("OPEN_CHAT_ACTIVITY") // Action bên Client xử lý
                                .build())
                        .build())
                .setWebpushConfig(WebpushConfig.builder()
                        .setNotification(WebpushNotification.builder()
                                .setIcon("/logo.png")
                                .build())
                        .build())
                .build();

        try {
            // Gửi và xử lý kết quả
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);

            // Kiểm tra token lỗi để xóa khỏi DB
            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                List<String> failedTokens = new ArrayList<>();
                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        // Nếu lỗi là do Token không hợp lệ (User đã logout/gỡ app)
                        MessagingErrorCode errorCode = responses.get(i).getException().getMessagingErrorCode();
                        if (MessagingErrorCode.UNREGISTERED.equals(errorCode) ||
                                MessagingErrorCode.INVALID_ARGUMENT.equals(errorCode)) {
                            failedTokens.add(tokens.get(i));
                        }
                    }
                }
                if (!failedTokens.isEmpty()) {
                    removeInvalidTokens(failedTokens);
                }
            }
        } catch (FirebaseMessagingException e) {
            e.printStackTrace();
        }
    }
    @Override
    public boolean sendTestNotification(Integer userId, String title, String body) {
        List<UserDeviceToken> deviceTokens = tokenRepository.findByAccount_AccountId(userId);
        if (deviceTokens.isEmpty()) {
            System.out.println("User ID " + userId + " không có token nào.");
            return false;
        }

        List<String> tokens = deviceTokens.stream()
                .map(UserDeviceToken::getToken)
                .collect(Collectors.toList());

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                // Gửi cả Notification và Data để đảm bảo hiện thông báo
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putData("type", "TEST_MANUAL")
                .build();

        try {
            FirebaseMessaging.getInstance().sendEachForMulticast(message);
            System.out.println("Đã gửi test thành công cho User ID: " + userId);
            return true;
        } catch (FirebaseMessagingException e) {
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
