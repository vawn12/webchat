package com.bkav.webchat.controller;

import com.bkav.webchat.dto.ApiResponse;
import com.bkav.webchat.dto.ContactResponseDTO;
import com.bkav.webchat.dto.m.ConversationDTO;
import com.bkav.webchat.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<ConversationDTO>>> getConversationsByType(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @PathVariable("type") String type) {
        List<ConversationDTO> list = conversationService.getConversationsByType(token, type);
        return ResponseEntity.ok(ApiResponse.success("Fetched " + type + " conversations", list));
    }
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<ConversationDTO>> createGroupConversation(
            @RequestHeader("Authorization") String token,
            @RequestParam String name,
            @RequestBody List<Integer> participantIds) {
        ConversationDTO dto = conversationService.createGroupConversation(token, name, participantIds);
        return ResponseEntity.ok(ApiResponse.success("Tạo nhóm thành công", dto));
    }
    @PostMapping("/private/{friendId}")
    public ResponseEntity<ApiResponse<ConversationDTO>> createPrivate(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Integer friendId) {

        ConversationDTO dto = conversationService.createPrivateConversation(authorization, friendId);
        return ResponseEntity.ok(ApiResponse.success("Tạo đoạn chat riêng tư thành công", dto));
    }

    @PostMapping("/{conversationId}/add_members")
    public ResponseEntity<ApiResponse<ConversationDTO>> addMembersToGroup(
            @RequestHeader("Authorization") String token,
            @PathVariable Integer conversationId,
            @RequestBody List<Integer> memberIds) {

        ConversationDTO dto = conversationService.addMembersToGroup(token, conversationId, memberIds);
        return ResponseEntity.ok(ApiResponse.success("Thêm thành viên thành công", dto));
    }
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ContactResponseDTO> contacts = conversationService.getAllConversation(page, size);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", 1);
        response.put("message", "Lấy danh sách chat thành công.");
        response.put("data", contacts.getContent());
        response.put("page", contacts.getNumber());
        response.put("size", contacts.getSize());
        response.put("totalPages", contacts.getTotalPages());
        response.put("totalElements", contacts.getTotalElements());

        return ResponseEntity.ok(response);
    }
    //chi tiết 1 cuộc trò chuyện
    @GetMapping("/{conversationId}")
    public ResponseEntity<ApiResponse<ConversationDTO>> getConversationDetails(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @PathVariable Integer conversationId) {

        ConversationDTO dto = conversationService.getConversationDetails(token, conversationId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết cuộc trò chuyện thành công", dto));
    }
    //rời cuộc tro chuyện
    @PostMapping("/{conversationId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveGroup(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @PathVariable Integer conversationId) {

        conversationService.leaveGroup(token, conversationId);
        return ResponseEntity.ok(ApiResponse.success("Rời nhóm thành công", null));
    }

}
