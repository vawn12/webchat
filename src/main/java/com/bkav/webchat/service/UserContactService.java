package com.bkav.webchat.service;

import com.bkav.webchat.dto.response.ContactResponseDTO;
import org.springframework.data.domain.Page;

import java.util.Map;


public interface UserContactService {

    Page<ContactResponseDTO> getAllContacts(int page, int size);
//    Page<ContactResponseDTO> searchContacts(String keyword, int page, int size);
    Page<ContactResponseDTO> searchContacts(String keyword, int page, int size, String username);
    Map<String, Object> searchFriendsSpecific(String keyword, int page, int size, String username);
}
