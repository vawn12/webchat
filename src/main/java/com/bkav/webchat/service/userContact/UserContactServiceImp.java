package com.bkav.webchat.service.userContact;

import com.bkav.webchat.dto.UserContactDTO;
import com.bkav.webchat.entity.UserContact;
import com.bkav.webchat.repository.UserContactRepository;
import com.bkav.webchat.service.account.AccountService;
import com.bkav.webchat.service.conversation.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserContactServiceImp {
    @Autowired
    private AccountService accountService;
    @Autowired
    private UserContactRepository userContactRepository;
    public UserContactDTO convertToDTO(UserContact entity) {
        return UserContactDTO.builder()
                .contactId(entity.getContactId())
                .owner(accountService.convertToDTO(entity.getOwner()))
                .contactUser(accountService.convertToDTO(entity.getContactUser()))
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public UserContact convertToEntity(UserContactDTO dto) {
        return UserContact.builder()
                .contactId(dto.getContactId())
                .owner(accountService.convertToEntity(dto.getOwner()))
                .contactUser(accountService.convertToEntity(dto.getContactUser()))
                .status(dto.getStatus())
                .createdAt(dto.getCreatedAt())
                .build();
    }
    public UserContactDTO findbyId(Integer id) {
        UserContact save=userContactRepository.findById(id).orElse(null);
        return convertToDTO(save);
    }
}
