package com.bkav.webchat.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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

}


