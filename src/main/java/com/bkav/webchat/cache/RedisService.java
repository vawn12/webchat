package com.bkav.webchat.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final String ACCOUNT_PREFIX = "account:";
    private static final String TOKEN_PREFIX = "jwt_token:";

    public void save(String key, Object value, long timeoutMinutes) {
        redisTemplate.opsForValue().set(key, value, timeoutMinutes, TimeUnit.MINUTES);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteToken(String token) {
        String key = TOKEN_PREFIX + token;
        redisTemplate.delete(key);
    }

    public void saveToken(String token, String username) {
        String key = TOKEN_PREFIX + token;
        // Token hợp lệ trong 24 giờ
        redisTemplate.opsForValue().set(key, username, 24, TimeUnit.HOURS);
    }
    public boolean isTokenValid(String token) {
        String key = TOKEN_PREFIX + token;
        return redisTemplate.hasKey(key);
    }
    public String getUsernameFromToken(String token) {
        String key = TOKEN_PREFIX + token;
        Object username = redisTemplate.opsForValue().get(key);
        return username != null ? username.toString() : null;
    }

    public void deleteUserCache(String username) {
        String cacheKey = ACCOUNT_PREFIX + username;
        redisTemplate.delete(cacheKey);
    }

    // Quản lý trạng thái Online/Offline
    public void saveOnlineStatus(String username) {
        redisTemplate.opsForValue().set("ONLINE:" + username, "true");
    }

    public void removeOnlineStatus(String username) {
        redisTemplate.delete("ONLINE:" + username);
    }

//    public boolean isUserOnline(String username) {
//        return Boolean.TRUE.equals(redisTemplate.hasKey("ONLINE:" + username));
//    }

    // Quản lý đếm tin nhắn chưa đọc (Unread Count)
    public long incrementUnreadNotification(Integer userId) {
        String key = "NOTI_UNREAD_COUNT:" + userId;
        Long val = stringRedisTemplate.opsForValue().increment(key);
        return val != null ? val : 0;
    }

    public void resetUnreadNotification(Integer userId) {
        String key = "NOTI_UNREAD_COUNT:" + userId;
        stringRedisTemplate.delete(key);
    }

    //  Quản lý Cooldown (Khoảng thời gian chờ giữa 2 lần thông báo)
 // Trả về true nếu đưọc phép gửi (chưa bị khóa), false nếu đang trong thời gian chờ
    public boolean checkAndSetPushCooldown(Integer userId, long minutes) {
        String key = "PUSH_COOLDOWN:" + userId;
        // Nếu key chưa có thì set và trả về true. Nếu có rồi thì trả về false.
        Boolean isSet = redisTemplate.opsForValue().setIfAbsent(key, "locked", minutes, TimeUnit.MINUTES);
        return Boolean.TRUE.equals(isSet);
    }
    // Kiểm tra danh sách User ai đang Online
    public List<String> getOnlineUsers(List<String> usernames) {
        List<String> keys = usernames.stream()
                .map(u -> "ONLINE:" + u)
                .collect(Collectors.toList());

        // Lấy hàng loạt giá trị từ Redis
        List<Object> results = redisTemplate.opsForValue().multiGet(keys);

        List<String> onlineUsernames = new ArrayList<>();
        for (int i = 0; i < usernames.size(); i++) {
            if (results.get(i) != null) { // Nếu có value trong Redis nghĩa là Online
                onlineUsernames.add(usernames.get(i));
            }
        }
        return onlineUsernames;
    }
    // CÁC HÀM DÙNG ĐỂ TEST THỬ

    //  Hack số lượng tin nhắn chưa đọc (Để test trường hợp > 1)
    public void setUnreadNotification(Integer userId, long count) {
        String key = "NOTI_UNREAD_COUNT:" + userId;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(count));
    }

    // Xóa Cooldown ngay lập tức (Để không phải chờ 2 phút)
    public void clearPushCooldown(Integer userId) {
        String key = "PUSH_COOLDOWN:" + userId;
        redisTemplate.delete(key);
    }

    // Force Offline (Để test gửi thông báo ngay cả khi đang bật socket)
    public void forceOffline(String username) {
        redisTemplate.delete("ONLINE:" + username);
    }

}


