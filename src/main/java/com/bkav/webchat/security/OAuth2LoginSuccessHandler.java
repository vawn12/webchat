package com.bkav.webchat.security;

import com.bkav.webchat.entity.Account;
import com.bkav.webchat.enumtype.Account_status;
import com.bkav.webchat.repository.AccountRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String avatarUrl = null;

        // Lấy avatar từ provider
        if (oAuth2User.getAttributes().containsKey("picture")) {
            Object picture = oAuth2User.getAttributes().get("picture");
            if (picture instanceof String) {
                avatarUrl = (String) picture;
            } else if (picture instanceof java.util.Map<?, ?> map) {
                avatarUrl = map.get("data") != null ? ((Map<?, ?>) map.get("data")).get("url").toString() : null;
            }
        }

        Optional<Account> existing = accountRepository.findByEmail(email);

        if (existing.isEmpty()) {
            Account account = new Account();
            account.setEmail(email);
            account.setDisplayName(name);
            account.setAvatarUrl(avatarUrl);
            account.setStatus(Account_status.ONLINE);
            accountRepository.save(account);
        }

        response.sendRedirect("/home");
    }
}
