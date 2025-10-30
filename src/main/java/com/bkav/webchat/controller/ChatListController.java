package com.bkav.webchat.controller;

import com.bkav.webchat.dto.ApiResponse;
import com.bkav.webchat.dto.ChatListDTO;
import com.bkav.webchat.dto.request.LoginRequest;
import com.bkav.webchat.entity.Account;
import com.bkav.webchat.service.AccountService;
import com.bkav.webchat.service.ChatListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contacts")
public class ChatListController {
    @Autowired
    private ChatListService chatListService;
    @Autowired
    private AccountService accountService;

//    @PostMapping("/contacts")
//    public ResponseEntity<ApiResponse<?>> getAllFriend(Map<String, String> body) {
//        return chatListService.getChatList(List.of());
//    }
        @GetMapping("")
        public List<Account> getAllUsers() {
    return accountService.getAllAccount();
}
}
