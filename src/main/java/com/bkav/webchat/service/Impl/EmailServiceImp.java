package com.bkav.webchat.service.Impl;

import com.bkav.webchat.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.List;

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


            String pathImage = "static/images/bkav.png";
            ClassPathResource imageResource = new ClassPathResource(pathImage);

            // Kiểm tra xem file có thật sự tồn tại trong Docker không
            if (imageResource.exists()) {
                helper.addInline("logo", imageResource);
            } else {

                System.err.println("Không tìm thấy file ảnh tại: " + pathImage);
            }

        } catch (Exception e) {
            System.err.println(" Lỗi gửi mail: " + e.getMessage());
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
