package com.bkav.webchat.controller;

import com.bkav.webchat.dto.response.ApiResponse;
import com.bkav.webchat.dto.response.ContactResponseDTO;
import com.bkav.webchat.security.JwtService;
import com.bkav.webchat.service.UserContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/contacts")
public class UserContactController {

    @Autowired
    private UserContactService userContactService;
    @Autowired
    private JwtService jwtService;


    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchContacts(
            @RequestParam ("q")String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String token) {
        String username = jwtService.extractUsername(token.substring(7));
        Page<ContactResponseDTO> results = userContactService.searchContacts(keyword, page, size,username);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", 1);
        response.put("message", "Tìm kiếm thành công.");
        response.put("data", results.getContent());
        response.put("page", results.getNumber());
        response.put("size", results.getSize());
        response.put("totalPages", results.getTotalPages());
        response.put("totalElements", results.getTotalElements());

        return ResponseEntity.ok(response);
    }
    @GetMapping("search-friend")
    public ResponseEntity<Map<String, Object>> searchFriends(
            @RequestParam("q") String keyword,
            @RequestParam(defaultValue = "0")int page,
            @RequestParam(defaultValue = "10")int size,
            @RequestHeader("Authorization") String authorization){
        String username = jwtService.extractUsername(authorization.substring(7));
        Map<String, Object> result =userContactService.searchFriendsSpecific(keyword,page,size,username);
    return ResponseEntity.ok(result);

    }
}
