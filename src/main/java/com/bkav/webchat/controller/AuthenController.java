package com.bkav.webchat.controller;

import com.bkav.webchat.dto.AccountDTO;
import com.bkav.webchat.dto.ForgotPasswordDTO;
import com.bkav.webchat.dto.VerifyTokenDTO;
import com.bkav.webchat.enumtype.Account_status;
import com.bkav.webchat.security.AccountDetailService;
import com.bkav.webchat.service.account.AccountService;
import com.bkav.webchat.service.email.EmailService;
import com.bkav.webchat.service.verifytoken.ForgotService;
import com.bkav.webchat.service.verifytoken.VerifyTokenService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    @Autowired
    private VerifyTokenService verifytokenService;

    @Autowired
    private VerifyTokenService verifyTokenService;
    @Autowired
    private AccountDetailService accountDetailService;

    @Value("${app.base-url}")
    private String baseUrl;
    @GetMapping("/login")
    public String loginPage(Model model) {
        return "common/login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) { // <-- Thay đổi ở đây
        System.out.println("====== ĐÃ GỌI PHƯƠNG THỨC doLogin TRONG CONTROLLER ======");
        AccountDTO account = accountService.login(username, password);
        if (account == null) {
            // Sử dụng addFlashAttribute để gửi thông báo qua redirect
            redirectAttributes.addFlashAttribute("error", "Sai tên tài khoản hoặc mật khẩu!");
            return "redirect:/login"; // <-- Thay đổi ở đây
        }

        session.setAttribute("account", account);
        return "redirect:/home";
    }

    @GetMapping("/register")
    public String register(Model model) {
        return "common/register";
    }

    @GetMapping("/forgot")
    public String forgot(Model model ) {return "common/forgot";}

    @PostMapping("/forgot")
    public String sendOtp(@RequestParam String email, HttpSession session, RedirectAttributes redirect) {
        AccountDTO account = accountService.findAccountByEmail(email.trim());
        if (account == null) {
            redirect.addFlashAttribute("error", "Email not found!");
            return "redirect:/forgot";
        }

        List<Integer> otpDigits = generateOtpDigits();
        String otp = convertDigitsToString(otpDigits);

        ForgotPasswordDTO forgot = ForgotPasswordDTO.builder()
                .accountId(account.getAccountId())
                .token(otp)
                .expiryDate(new Date(System.currentTimeMillis() + 5 * 60 * 1000))
                .build();

        forgotPasswordService.createForgotPassword(forgot);
        String content = emailService.builEmailContentForResetPassword(otpDigits);
        emailService.sendMailTime(email, "Password Reset OTP", content);
        session.setAttribute("resetEmail", email);

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
            String otp = convertDigitsToString(otpDigits);
            forgotPasswordDTO = ForgotPasswordDTO.builder().id(account.getAccountId()).token(otp).expiryDate(new Date(System.currentTimeMillis() + 5 * 60 * 1000)).build();
            forgotPasswordService.createForgotPassword(forgotPasswordDTO);
            String content = emailService.builEmailContentForResetPassword(otpDigits);
            emailService.sendMailTime(email, "Reset Password", content);
            return "common/verify-otp";
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid email");
            return "redirect:/forgot";
        }
    }
    @GetMapping("/verifytoken")
    public String verifyToken(@RequestParam("token") String token, HttpSession session, Model model) {
        VerifyTokenDTO verifyTokenDTO = verifyTokenService.findByToken(token);
        if (verifyTokenDTO == null) {
            model.addAttribute("error", "Invalid token");
            return "common/register";
        } else if (verifyTokenDTO.getExpiresAt().before(new Date(System.currentTimeMillis()))) {
            verifyTokenService.deleteTokenByEmail(verifyTokenDTO.getEmail());
            model.addAttribute("error", "Expired token");
            return "common/register";
        } else {
            AccountDTO accountDTO = AccountDTO.builder().email(verifyTokenDTO.getEmail()).password(verifyTokenDTO.getPassword()).status(Account_status.ONLINE).build();
            AccountDTO accountDTO1 = accountService.save(accountDTO);
            session.setAttribute("account", accountDTO1);
            verifyTokenService.deleteTokenByEmail(verifyTokenDTO.getEmail());
            UserDetails userDetails = accountDetailService.loadUserByUsername(accountDTO.getEmail());
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return "redirect:/";
        }
    }
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirect) {
        session.invalidate();
        redirect.addFlashAttribute("message", "Logged out successfully");
        return "redirect:/login";
    }
    @PostMapping("/register")
    public String doRegister(@RequestParam String email,
                             @RequestParam String password,
                             @RequestParam String fullname,
                             RedirectAttributes redirect) {

        AccountDTO existing = accountService.findAccountByEmail(email.trim());
        if (existing != null) {
            redirect.addFlashAttribute("error", "Email already registered!");
            return "redirect:/register";
        }

        VerifyTokenDTO oldToken = verifyTokenService.getTokenByEmail(email.trim());
        if (oldToken != null) {
            verifyTokenService.deleteTokenByEmail(email.trim());
        }

        String token = UUID.randomUUID().toString();
        VerifyTokenDTO verifyToken = VerifyTokenDTO.builder()
                .email(email.trim())
                .token(token)
                .expiresAt(new Date(System.currentTimeMillis() + 10 * 60 * 1000))
                .fullName(fullname.trim())
                .password(password.trim())
                .build();

        verifyTokenService.create(verifyToken);

        String verifyLink = baseUrl + "/verify?token=" + token;
        String subject = "Email Verification";
        String content = emailService.buildEmailContent(fullname, verifyLink);
        emailService.sendMailTime(email, subject, content);

        redirect.addFlashAttribute("inform", "Please check your email to verify your account.");
        return "redirect:/login";
    }
    @GetMapping("/verify")
    public String verifyAccount(@RequestParam("token") String token,
                                HttpSession session,
                                RedirectAttributes redirect) {
        VerifyTokenDTO verifyToken = verifyTokenService.findByToken(token);
        if (verifyToken == null) {
            redirect.addFlashAttribute("error", "Invalid verification token!");
            return "redirect:/register";
        }
        if (verifyToken.getExpiresAt().before(new Date())) {
            verifyTokenService.deleteTokenByEmail(verifyToken.getEmail());
            redirect.addFlashAttribute("error", "Token expired!");
            return "redirect:/register";
        }

        AccountDTO dto = AccountDTO.builder()
                .username(verifyToken.getEmail().split("@")[0])
                .email(verifyToken.getEmail())
                .displayName(verifyToken.getFullName())
                .status(Account_status.OFFLINE)
                .build();

        accountService.register(dto, verifyToken.getPassword());
        verifyTokenService.deleteTokenByEmail(verifyToken.getEmail());
        redirect.addFlashAttribute("success", "Account verified successfully! You can now log in.");
        return "redirect:/login";
    }
    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String otp, HttpSession session, RedirectAttributes redirect) {
        String email = (String) session.getAttribute("resetEmail");
        if (email == null) {
            redirect.addFlashAttribute("error", "Session expired");
            return "redirect:/forgot";
        }

        AccountDTO account = accountService.findAccountByEmail(email);
        ForgotPasswordDTO forgot = forgotPasswordService.findForgotPasswordByAccountId(account.getAccountId());
        if (forgot == null || !forgot.getToken().equals(otp)) {
            redirect.addFlashAttribute("error", "Invalid OTP");
            return "redirect:/forgot";
        }
        if (forgot.getExpiryDate().before(new Date())) {
            redirect.addFlashAttribute("error", "OTP expired");
            return "redirect:/forgot";
        }
        session.setAttribute("otpVerified", true);
        return "common/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String newPassword,
                                HttpSession session,
                                RedirectAttributes redirect) {
        Boolean verified = (Boolean) session.getAttribute("otpVerified");
        String email = (String) session.getAttribute("resetEmail");
        if (verified == null || !verified || email == null) {
            redirect.addFlashAttribute("error", "Unauthorized action");
            return "redirect:/login";
        }

        AccountDTO account = accountService.findAccountByEmail(email);
        accountService.register(account, newPassword);
        forgotPasswordService.deleteForgotPassword(account.getAccountId());

        session.invalidate();
        redirect.addFlashAttribute("success", "Password changed successfully");
        return "redirect:/login";
    }
    public int convertDigitsToInteger(List<Integer> otpDigits) {
        StringBuilder sb = new StringBuilder();
        for (Integer digit : otpDigits) {
            sb.append(digit);
        }
        return Integer.parseInt(sb.toString());
    }
    public String convertDigitsToString(List<Integer> otpDigits) {
        StringBuilder sb = new StringBuilder();
        for (Integer digit : otpDigits) {
            sb.append(digit);
        }
        return sb.toString();
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
