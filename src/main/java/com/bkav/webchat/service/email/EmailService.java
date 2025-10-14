package com.bkav.webchat.service.email;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
@Service
public interface EmailService {
    void sendMailTime(String to, String subject, String content);
    String buildEmailContent(String username, String link);
    String builEmailContentForResetPassword(List<Integer> otp);

}
