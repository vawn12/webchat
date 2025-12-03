package com.bkav.webchat.controller;

import com.bkav.webchat.dto.ApiResponse;
import com.bkav.webchat.entity.Account;
import com.bkav.webchat.entity.UserDeviceToken;
import com.bkav.webchat.repository.UserDeviceTokenRepository;
import com.bkav.webchat.service.AccountService;
import com.bkav.webchat.service.FirebasePushService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;
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
}
