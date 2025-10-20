package com.bkav.webchat.controller;

import com.bkav.webchat.dto.ChatListDTO;
import com.bkav.webchat.entity.Account;
import com.bkav.webchat.service.ChatListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
@Controller
@RequestMapping("chat")
public class ChatListController {
    @Autowired
    private ChatListService chatListService;

    @GetMapping("/list/{userId}")
    public List<ChatListDTO> getChatList(@PathVariable Long userId) {
        return chatListService.getChatList(userId);
    }
    @GetMapping("")
    public String showChatListPage(@AuthenticationPrincipal Account account, Model model) {
        //@AuthenticationPrincipal sẽ tự map tài khoản đang đăng nhập
        Integer userId = account != null ? account.getAccountId() : null;

        model.addAttribute("userId", userId);
        return "common/chat-list";
    }
}
