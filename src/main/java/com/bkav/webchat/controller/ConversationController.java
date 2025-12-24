package com.bkav.webchat.controller;

import com.bkav.webchat.dto.response.ApiResponse;
import com.bkav.webchat.dto.response.ContactResponseDTO;
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
    //tạo nhóm
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<ConversationDTO>> createGroupConversation(
            @RequestHeader("Authorization") String token,
            @RequestParam String name,
            @RequestBody List<Integer> participantIds) {
        ConversationDTO dto = conversationService.createGroupConversation(token, name, participantIds);
        return ResponseEntity.ok(ApiResponse.success("Tạo nhóm thành công", dto));
    }
    //tao cuộc trò chuyện nhóm
    @PostMapping("/private/{friendId}")
    public ResponseEntity<ApiResponse<ConversationDTO>> createPrivate(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Integer friendId) {

        ConversationDTO dto = conversationService.createPrivateConversation(authorization, friendId);
        return ResponseEntity.ok(ApiResponse.success("Tạo đoạn chat riêng tư thành công", dto));
    }
    //thêm thành viên vào nhóm
    @PostMapping("/{conversationId}/add_members")
    public ResponseEntity<ApiResponse<ConversationDTO>> addMembersToGroup(
            @RequestHeader("Authorization") String token,
            @PathVariable Integer conversationId,
            @RequestBody List<Integer> memberIds) {

        ConversationDTO dto = conversationService.addMembersToGroup(token, conversationId, memberIds);
        return ResponseEntity.ok(ApiResponse.success("Thêm thành viên thành công", dto));
    }

    //lấy danh sách nhóm
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> getConversations(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ContactResponseDTO> contacts = conversationService.getAllConversation(token,page, size);
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
    // Đôi tên nhóm
    @PutMapping("/{conversationId}/rename")
    public ResponseEntity<ConversationDTO> renameGroup(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Integer conversationId,
            @RequestParam String newName) {

        ConversationDTO updatedConversation = conversationService.renameGroupConversation(
                authorizationHeader,
                conversationId,
                newName
        );

        return ResponseEntity.ok(updatedConversation);
    }
    //rời cuộc tro chuyện
    @PostMapping("/{conversationId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveGroup(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @PathVariable Integer conversationId) {

        conversationService.leaveGroup(token, conversationId);
        return ResponseEntity.ok(ApiResponse.success("Rời nhóm thành công", null));
    }
    // Xóa thành viên khỏi nhóm

    @PostMapping("/{conversationId}/remove_members")
    public ResponseEntity<ApiResponse<ConversationDTO>> removeMembersFromGroup(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @PathVariable Integer conversationId,
            @RequestBody List<Integer> memberIds) { // Nhận list ID từ Body

        ConversationDTO updatedGroup = conversationService.removeMemberFromGroup(token, conversationId, memberIds);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa các thành viên khỏi nhóm thành công", updatedGroup));
    }
}
