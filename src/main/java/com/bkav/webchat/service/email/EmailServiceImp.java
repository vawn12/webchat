package com.bkav.webchat.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.List;
import java.util.Map;

@Service
public class EmailServiceImp implements EmailService {
    @Autowired
    private SpringTemplateEngine templateEngine;
    @Autowired
    private JavaMailSender mailSender;
    @Override
    public void sendMailTime(String to, String subject, String content) {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper;
        try {
            helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            helper.addInline("logo", new ClassPathResource("static/customer-static/images/bkav.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        mailSender.send(message);
    }
    @Override
    public String buildEmailContent(String username, String link) {
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("link", link);

        return templateEngine.process("common/email-confirmation", context);
    }
    @Override
    public String builEmailContentForResetPassword(List<Integer> otp) {
        Context context = new Context();
        context.setVariable("otp", otp);

        return templateEngine.process("common/otp-confirmation", context);
    }
}
