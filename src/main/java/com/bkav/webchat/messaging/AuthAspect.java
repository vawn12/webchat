package com.bkav.webchat.messaging;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuthAspect {

    @Pointcut("@annotation(com.bkav.webchat.messaging.RequireAuth)")
    public void requireAuthMethods() {}

    @Before("requireAuthMethods()")
    public void checkAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            throw new RuntimeException("Chưa xác thực (missing token)");
        }
    }
}

