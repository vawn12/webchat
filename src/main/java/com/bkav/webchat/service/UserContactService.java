package com.bkav.webchat.service;

import com.bkav.webchat.dto.response.ContactResponseDTO;
import org.springframework.data.domain.Page;


public interface UserContactService {

    Page<ContactResponseDTO> getAllContacts(int page, int size);
//    Page<ContactResponseDTO> searchContacts(String keyword, int page, int size);
    Page<ContactResponseDTO> searchContacts(String keyword, int page, int size, String username);

}
