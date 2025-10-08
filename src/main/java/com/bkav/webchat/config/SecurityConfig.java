package com.bkav.webchat.config;

import com.bkav.webchat.security.AccountDetailService;
import com.bkav.webchat.security.CustomFailureHandler;
import com.bkav.webchat.security.CustomOauthFailureHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Autowired
    private AccountDetailService accountDetailService;

    @Autowired
    private CustomFailureHandler customFailureHandler;

    @Autowired
    private CustomOauthFailureHandler customOauthFailureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                //  Tắt CSRF để test dễ hơn (có thể bật lại khi deploy)
                .csrf(csrf -> csrf.disable())

                // Phân quyền các request
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/verify**", "/forgot**",
                                "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .anyRequest().authenticated()
                )

                // Cấu hình form login
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")  // action form
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .failureHandler(customFailureHandler)
                        .defaultSuccessUrl("/home", true)
                        .permitAll()
                )

                //  Cấu hình logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .deleteCookies("JSESSIONID", "LOGIN_ERROR")
                        .permitAll()
                );

                // (Tùy chọn) Cấu hình OAuth2 login
//                .oauth2Login(oauth -> oauth
//                        .loginPage("/login")
//                        .failureHandler(customOauthFailureHandler)
//                        .defaultSuccessUrl("/home", true)
//                );

        return http.build();
    }

    // ✅ Password encoder cho toàn hệ thống
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ✅ AuthenticationManager cho việc auth thủ công
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
