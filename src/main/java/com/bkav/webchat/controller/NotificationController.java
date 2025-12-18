package com.bkav.webchat.controller;

import com.bkav.webchat.cache.RedisService;
import com.bkav.webchat.dto.response.ApiResponse;
import com.bkav.webchat.repository.UserDeviceTokenRepository;
import com.bkav.webchat.service.AccountService;
import com.bkav.webchat.service.FirebasePushService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private UserDeviceTokenRepository tokenRepository;
    @Autowired
    private AccountService accountService;
    @Autowired
    private FirebasePushService firebasePushService;
    @Autowired
    private RedisService redisService;

    @PostMapping("/send")
    public ApiResponse<String> sendTest(@RequestBody Map<String, Object> request) {
        Integer userId = (Integer) request.get("userId");
        String title = (String) request.get("title");
        String body = (String) request.get("body");

        if (userId == null) {
            return ApiResponse.fail("Thiếu userId");
        }

        // Gọi service và nhận kết quả
        boolean isSent = firebasePushService.sendTestNotification(userId, title, body);
        if (isSent) {
            return ApiResponse.success("Đã gửi lệnh push notification thành công.", null);
        } else {
            return ApiResponse.fail("Gửi thất bại. User ID " + userId + " không tồn tại hoặc chưa đăng nhập (chưa có Token).");
        }
    }
    @PostMapping("/setup-noti")
    public ApiResponse<String> setupNotificationTest(
            @RequestParam Integer userId,
            @RequestParam int unreadCount,
            @RequestParam String username
    ) {
        // 1. Giả lập số tin nhắn đang chờ
        redisService.setUnreadNotification(userId, unreadCount);

        // 2. Xóa thời gian chờ (để tin nhắn tiếp theo được gửi đi ngay)
        redisService.clearPushCooldown(userId);

        // 3. Đảm bảo user đang Offline (để Consumer không skip)
        redisService.forceOffline(username);

        return ApiResponse.success("Đã setup: Unread=" + unreadCount + ", Cooldown=CLEARED, Status=OFFLINE", null);
    }
}
