package com.bkav.webchat.service.Impl;

import com.bkav.webchat.dto.response.ContactResponseDTO;
import com.bkav.webchat.entity.*;
import com.bkav.webchat.enumtype.ConversationType;
import com.bkav.webchat.repository.ConversationRepository;
import com.bkav.webchat.repository.MessageRepository;
import com.bkav.webchat.repository.UserContactRepository;
import com.bkav.webchat.service.AccountService;
import com.bkav.webchat.service.UserContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserContactServiceImp implements UserContactService {
    @Autowired
    private AccountService accountService;
    @Autowired
    private UserContactRepository userContactRepository;
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private MessageRepository messageRepository;
    // Lấy danh sách tất cả liên lạc
    public Page<ContactResponseDTO> getAllContacts(int page, int size) {
        List<ContactResponseDTO> allContacts = new ArrayList<>();
        //  Nhóm
        List<Conversation> groups = conversationRepository.findAllByType(ConversationType.GROUP);
        allContacts.addAll(groups.stream()
                .map(this::mapGroupToDTO)
                .collect(Collectors.toList()));
        //  Bạn bè
        List<UserContact> contacts = userContactRepository.findAllAccepted();
        allContacts.addAll(contacts.stream()
                .map(this::mapPrivateToDTO)
                .collect(Collectors.toList()));

        // Sắp xếp theo thời gian tin nhắn mới nhất
        allContacts.sort(Comparator.comparing(
                (ContactResponseDTO dto) -> Optional.ofNullable(dto.getLastMessageAt()).orElse(dto.getLastMessageAt())
        ).reversed());

        int start = Math.min(page * size, allContacts.size());
        int end = Math.min(start + size, allContacts.size());
        List<ContactResponseDTO> paged = allContacts.subList(start, end);

        return new PageImpl<>(paged, PageRequest.of(page, size), allContacts.size());
    }

    // Map nhóm
    private ContactResponseDTO mapGroupToDTO(Conversation c) {
        Message lastMsg = messageRepository.findTopByConversation_ConversationIdOrderByCreatedAtDesc(c.getConversationId());
        return ContactResponseDTO.builder()
                .id(c.getConversationId())
                .type("group")
                .name(c.getName())
                .avatarUrl("/uploads/groups/" + c.getConversationId() + ".png")
                .lastMessage(lastMsg != null ? lastMsg.getContent() : null)
                .lastMessageAt(lastMsg != null ? lastMsg.getCreatedAt() : c.getCreatedAt())
                .build();
    }

    // Map bạn bè
    private ContactResponseDTO mapPrivateToDTO(UserContact uc) {
        Account friend = uc.getContactUser();
        Conversation privateConv = conversationRepository.findPrivateConversationBetween(
                uc.getOwner().getAccountId(), friend.getAccountId());
        Message lastMsg = null;
        if (privateConv != null)
            lastMsg = messageRepository.findTopByConversation_ConversationIdOrderByCreatedAtDesc(privateConv.getConversationId());

        return ContactResponseDTO.builder()
                .id(privateConv != null ? privateConv.getConversationId() : uc.getContactId())
                .type("private")
                .contactUserId(friend.getAccountId())
                .contactUserName(friend.getDisplayName())
                .contactUserAvatar(friend.getAvatarUrl())
                .userStatus(friend.getStatus().name())
                .lastMessage(lastMsg != null ? lastMsg.getContent() : null)
                .lastMessageAt(lastMsg != null ? lastMsg.getCreatedAt() : uc.getCreatedAt())
                .build();
    }
    // Tìm kiếm nguoi liên hệ
    // Sửa lại signature của hàm để nhận thêm username
    public Page<ContactResponseDTO> searchContacts(String keyword, int page, int size, String username) {
        List<ContactResponseDTO> results = new ArrayList<>();

        // 1. Lấy thông tin người đang thực hiện tìm kiếm
        Account currentUser = accountService.getAccountEntityByUsername(username);
        if (currentUser == null) {
            throw new RuntimeException("User not found");
        }

        // 2. Tìm tất cả người dùng khớp từ khóa
        List<Account> accounts = accountService.findByKeyword(keyword);

        for (Account account : accounts) {
            // Bỏ qua chính mình trong kết quả tìm kiếm
            if (account.getAccountId().equals(currentUser.getAccountId())) {
                continue;
            }

            // Tạo UserContact giả lập đúng logic
            UserContact temp = new UserContact();
            temp.setContactUser(account); // Người được tìm thấy
            temp.setOwner(currentUser);   // Người đang tìm kiếm (QUAN TRỌNG)
            temp.setCreatedAt(java.time.LocalDateTime.now()); // Gán thời gian tạm để tránh lỗi Null khi sort

            // mapPrivateToDTO sẽ tự động tìm Conversation giữa CurrentUser và Account này
            results.add(mapPrivateToDTO(temp));
        }

        // 3. Tìm tất cả nhóm (Giữ nguyên)
        List<Conversation> groups = conversationRepository.findAllByTypeAndNameContainingIgnoreCase(
                ConversationType.GROUP, keyword);
        results.addAll(groups.stream()
                .map(this::mapGroupToDTO)
                .toList());

        // 4. Sắp xếp (Có sửa nhẹ để tránh NullPointerException)
        results.sort(Comparator.comparing(
                (ContactResponseDTO dto) -> Optional.ofNullable(dto.getLastMessageAt()).orElse(java.time.LocalDateTime.MIN),
                Comparator.nullsLast(Comparator.naturalOrder())
        ).reversed());

        int start = Math.min(page * size, results.size());
        int end = Math.min(start + size, results.size());
        List<ContactResponseDTO> paged = results.subList(start, end);

        return new PageImpl<>(paged, PageRequest.of(page, size), results.size());
    }

}
