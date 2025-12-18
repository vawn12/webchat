package com.bkav.webchat.config;

import com.bkav.webchat.cache.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
public class WebSocketEventListener {

    @Autowired
    private RedisService redisService;

    // Khi User kết nối WebSocket -> Đánh dấu Online
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        Principal user = event.getUser();
        if (user != null) {
            redisService.saveOnlineStatus(user.getName());
        }
    }

    // Khi User mất kết nối -> Đánh dấu Offline
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        Principal user = event.getUser();
        if (user != null) {
            redisService.removeOnlineStatus(user.getName());
        }
    }
}
