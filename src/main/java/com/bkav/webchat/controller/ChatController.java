package com.bkav.webchat.controller;

import com.bkav.webchat.dto.ApiResponse;
import com.bkav.webchat.dto.m.MessageResponseDTO;
import com.bkav.webchat.dto.request.ChatMessageRequest;
import com.bkav.webchat.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/message")
public class ChatController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessageService messageService;

    @PostMapping("/send/{conversationId}")
    public ResponseEntity<ApiResponse<MessageResponseDTO>> sendMessage(
            @PathVariable Integer conversationId,
            @RequestBody ChatMessageRequest request) {

        ApiResponse<MessageResponseDTO> response = messageService.sendMessage(conversationId, request);
        if (!response.isSuccess()) return ResponseEntity.badRequest().body(response);
        return ResponseEntity.ok(response);
    }
    //chỉnh sửa tin nhắn
    @PutMapping("/update/{conversationId}")
    public ResponseEntity<ApiResponse<MessageResponseDTO>> updateMessage(
            @PathVariable Integer conversationId,
            @RequestBody ChatMessageRequest request) {
        ApiResponse<MessageResponseDTO> response = messageService.updateMessage(conversationId, request);
        if (!response.isSuccess()) return ResponseEntity.badRequest().body(response);
        return ResponseEntity.ok(response);
    }

    // Xóa tin nhắn
    @DeleteMapping("/delete/{conversationId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable Integer conversationId,
            @RequestBody ChatMessageRequest request) {

        ApiResponse<Void> response = messageService.deleteMessage(conversationId, request);
        if (!response.isSuccess()) return ResponseEntity.badRequest().body(response);
        return ResponseEntity.ok(response);
    }


}
