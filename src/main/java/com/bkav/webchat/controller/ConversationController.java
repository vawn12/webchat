package com.bkav.webchat.controller;

import com.bkav.webchat.dto.ApiResponse;
import com.bkav.webchat.dto.m.ConversationDTO;
import com.bkav.webchat.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @GetMapping("")
    public ResponseEntity<ApiResponse<List<ConversationDTO>>> getAllConversations(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        List<ConversationDTO> list = conversationService.getConversationsByToken(token);
        return ResponseEntity.ok(ApiResponse.success("Fetched conversations successfully", list));
    }

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

    @PostMapping("/{conversationId}/add_members")
    public ResponseEntity<ApiResponse<ConversationDTO>> addMembersToGroup(
            @RequestHeader("Authorization") String token,
            @PathVariable Integer conversationId,
            @RequestBody List<Integer> memberIds) {

        ConversationDTO dto = conversationService.addMembersToGroup(token, conversationId, memberIds);
        return ResponseEntity.ok(ApiResponse.success("Thêm thành viên thành công", dto));
    }

}
