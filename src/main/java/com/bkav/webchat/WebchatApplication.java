package com.bkav.webchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import java.util.TimeZone;

@EntityScan("com.bkav.webchat.entity")
@SpringBootApplication
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
@EnableWebSocket
public class WebchatApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SpringApplication.run(WebchatApplication.class, args);
    }
}