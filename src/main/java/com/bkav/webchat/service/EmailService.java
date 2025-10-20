package com.bkav.webchat.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface EmailService {
    void sendMailTime(String to, String subject, String content);
    String buildEmailContent(String username, String link);
    String builEmailContentForResetPassword(List<Integer> otp);

}
