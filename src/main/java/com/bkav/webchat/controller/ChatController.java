package com.bkav.webchat.controller;

import com.bkav.webchat.dto.ApiResponse;
import com.bkav.webchat.dto.m.MessageResponseDTO;
import com.bkav.webchat.dto.request.ChatMessageRequest;
import com.bkav.webchat.dto.request.ReactionRequest;
import com.bkav.webchat.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;


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
        // principal có thể là null nếu user chưa auth
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("Chưa xác thực (missing token)."));
        }

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

        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("Chưa xác thực (missing token)."));
        }

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

        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("Chưa xác thực (missing token)."));
        }

        ApiResponse<Void> response = messageService.deleteMessage(conversationId, request, principal.getName());
        if (!response.isSuccess()) return ResponseEntity.badRequest().body(response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/react/{conversationId}")
    public ResponseEntity<ApiResponse<String>> reactToMessage(
            @PathVariable Integer conversationId,
            @RequestBody ReactionRequest request,
            Principal principal
    ) {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("Chưa xác thực (missing token)."));
        }

        ApiResponse<String> response = messageService.reactToMessage(conversationId, request, principal.getName());
        if (!response.isSuccess()) return ResponseEntity.badRequest().body(response);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/upload/{conversationId}")
    public ResponseEntity<ApiResponse<String>> uploadFile(
            @PathVariable Integer conversationId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("messageId") Integer messageId,
            Principal principal
    ) {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("Chưa xác thực (missing token)."));
        }

        ApiResponse<String> response = messageService.uploadAttachment(conversationId, file, messageId, principal.getName());
        if (!response.isSuccess()) return ResponseEntity.badRequest().body(response);
        return ResponseEntity.ok(response);
    }

}
