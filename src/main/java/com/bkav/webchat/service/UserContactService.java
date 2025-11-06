package com.bkav.webchat.service;

import com.bkav.webchat.dto.ContactResponseDTO;
import com.bkav.webchat.dto.m.UserContactDTO;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;


public interface UserContactService {

    Page<ContactResponseDTO> getAllContacts(int page, int size);
    Page<ContactResponseDTO> searchContacts(String keyword, int page, int size);

}
