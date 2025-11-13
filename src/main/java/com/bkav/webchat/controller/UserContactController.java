package com.bkav.webchat.controller;

import com.bkav.webchat.dto.ApiResponse;
import com.bkav.webchat.dto.ContactResponseDTO;
import com.bkav.webchat.dto.m.UserContactDTO;
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


    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchContacts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ContactResponseDTO> results = userContactService.searchContacts(keyword, page, size);

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

}
