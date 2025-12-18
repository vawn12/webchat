package com.bkav.webchat.controller;

import com.bkav.webchat.dto.response.ApiResponse;
import com.bkav.webchat.dto.response.MessageResponseDTO;
import com.bkav.webchat.dto.request.ChatMessageRequest;
import com.bkav.webchat.dto.request.ReactionRequest;
import com.bkav.webchat.entity.MessageDocument;
import com.bkav.webchat.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;


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
            @RequestBody ChatMessageRequest request,
             Principal principal
    ) {

        ApiResponse<MessageResponseDTO> response = messageService.sendMessage(conversationId, request, principal.getName());
        if (!response.isSuccess()) return ResponseEntity.badRequest().body(response);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update/{conversationId}")
    public ResponseEntity<ApiResponse<MessageResponseDTO>> updateMessage(
            @PathVariable Integer conversationId,
            @RequestBody ChatMessageRequest request,
            Principal principal
    ) {

        ApiResponse<MessageResponseDTO> response = messageService.updateMessage(conversationId, request, principal.getName());
        if (!response.isSuccess()) return ResponseEntity.badRequest().body(response);
        return ResponseEntity.ok(response);
    }

    // Xóa tin nhắn
    @DeleteMapping("/delete/{conversationId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable Integer conversationId,
            @RequestBody ChatMessageRequest request,
            Principal principal
    ) {

        ApiResponse<Void> response = messageService.deleteMessage(conversationId, request, principal.getName());
        if (!response.isSuccess()) return ResponseEntity.badRequest().body(response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/react/{conversationId}")
    public ResponseEntity<?> reactToMessage(
            @PathVariable Integer conversationId,
            @RequestBody ReactionRequest request,
            Principal principal
    ) {

        ApiResponse<String> response =
                messageService.reactToMessage(conversationId, request, principal.getName());
        return ResponseEntity.ok(response);
    }
    @PostMapping("/upload/{conversationId}")
    public ResponseEntity<ApiResponse<MessageResponseDTO>> uploadFile(
            @PathVariable Integer conversationId,
            @RequestParam("file") MultipartFile file,
            Principal principal
    ) {

        ApiResponse<MessageResponseDTO> response = messageService.uploadAttachment(conversationId, file,  principal.getName());
        if (!response.isSuccess()) return ResponseEntity.badRequest().body(response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<MessageDocument>>> searchMessages(
            @RequestParam("q") String query,
            Principal principal
    ) {

        ApiResponse<List<MessageDocument>> response = messageService.searchMessages(query, principal.getName());

        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }
    @PostMapping("/mark-read/{conversationId}")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Integer conversationId,
            Principal principal
    ) {
        ApiResponse<Void> response = messageService.markAsRead(conversationId, principal.getName());
        return ResponseEntity.ok(response);
    }
}
