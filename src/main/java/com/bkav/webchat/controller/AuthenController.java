package com.bkav.webchat.controller;

import com.bkav.webchat.dto.AccountDTO;
import com.bkav.webchat.dto.ForgotPasswordDTO;
import com.bkav.webchat.enumtype.Account_status;
import com.bkav.webchat.service.account.AccountService;
import com.bkav.webchat.service.email.EmailService;
import com.bkav.webchat.service.verifytoken.ForgotService;
import com.bkav.webchat.service.verifytoken.VerifyTokenService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Controller
@RequestMapping
public class AuthenController {
    @Autowired
    private AccountService accountService;
    @Autowired
    private ForgotService forgotPasswordService;
    @Autowired
    private EmailService emailService;
    @Value("${app.base-url}")
    private String baseUrl;

    @GetMapping("/login")
    public String login(Model model, @CookieValue(name = "LOGIN_ERROR", required = false) String loginError, HttpServletResponse response) {
        if (loginError != null) {
            try {
                String decodedError = URLDecoder.decode(loginError, StandardCharsets.UTF_8);
                model.addAttribute("error", decodedError);
            } catch (Exception e) {
                model.addAttribute("error", "Unknown error");
            }

            Cookie cookie = new Cookie("LOGIN_ERROR", null);
            cookie.setMaxAge(0);
            cookie.setPath("/");
            response.addCookie(cookie);
        }
        return "common/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        return "common/register";
    }

    @GetMapping("/forgot")
    public String forgot(Model model ) {return "common/forgot";}
    @GetMapping("/resendotp")
    public String resendotp(@RequestParam("email") String email, HttpSession session, Model model) {
        AccountDTO account = (AccountDTO) session.getAttribute("account");
        ForgotPasswordDTO forgotPasswordDTO = forgotPasswordService.findForgotPasswordByAccountId(account.getAccountId());
        if (forgotPasswordDTO != null) {
            forgotPasswordService.deleteForgotPassword(forgotPasswordDTO.getId());
        }
        List<Integer> otpDigits = generateOtpDigits();
        int otp = convertDigitsToInteger(otpDigits);
        forgotPasswordDTO = ForgotPasswordDTO.builder().id(account.getAccountId()).token(otp).expiryDate(new Date(System.currentTimeMillis() + 5 * 60 * 1000)).build();
        forgotPasswordService.createForgotPassword(forgotPasswordDTO);
        String content = emailService.builEmailContentForResetPassword(otpDigits);
        emailService.sendMailTime(email, "Reset Password", content);
        return "common/verify-otp";
    }
    @GetMapping("/verifyemail")
    public String verifyemail(@RequestParam("email") String email, RedirectAttributes redirectAttributes, Model model, HttpSession session) {
        AccountDTO account = accountService.findAccountByEmail(email);
        if (account != null && account.getStatus() == Account_status.ONLINE) {
            session.setAttribute("account", account);
            model.addAttribute("email", email);
            ForgotPasswordDTO forgotPasswordDTO = forgotPasswordService.findForgotPasswordByAccountId(account.getAccountId());
            if (forgotPasswordDTO != null) {
                if (forgotPasswordDTO.getExpiryDate().before(new Date(System.currentTimeMillis()))) {
                    forgotPasswordService.deleteForgotPassword(forgotPasswordDTO.getId());
                } else {
                    return "common/verify-otp";
                }
            }
            List<Integer> otpDigits = generateOtpDigits();
            int otp = convertDigitsToInteger(otpDigits);
            forgotPasswordDTO = ForgotPasswordDTO.builder().id(account.getAccountId()).token(otp).expiryDate(new Date(System.currentTimeMillis() + 5 * 60 * 1000)).build();
            forgotPasswordService.createForgotPassword(forgotPasswordDTO);
            String content = emailService.builEmailContentForResetPassword(otpDigits);
            emailService.sendMailTime(email, "Reset Password", content);
            return "common/verify-otp";
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid email");
            return "redirect:/forgotpassword";
        }
    }
    public int convertDigitsToInteger(List<Integer> otpDigits) {
        StringBuilder sb = new StringBuilder();
        for (Integer digit : otpDigits) {
            sb.append(digit);
        }
        return Integer.parseInt(sb.toString());
    }
    public List<Integer> generateOtpDigits() {
        Random rand = new Random();
        List<Integer> otpDigits = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            otpDigits.add(rand.nextInt(10));
        }
        return otpDigits;
    }
}
