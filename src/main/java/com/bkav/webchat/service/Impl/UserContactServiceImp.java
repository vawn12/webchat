package com.bkav.webchat.service.Impl;

import com.bkav.webchat.dto.m.FriendSearchDTO;
import com.bkav.webchat.dto.response.ContactResponse;
import com.bkav.webchat.dto.response.ContactResponseDTO;
import com.bkav.webchat.dto.response.SearchFriendResponse;
import com.bkav.webchat.entity.*;
import com.bkav.webchat.enumtype.ConversationType;
import com.bkav.webchat.repository.AttachmentRepository;
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

import java.time.LocalDateTime;
import java.util.*;
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
    @Autowired
    private AttachmentRepository attachmentRepository;

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


    public Map<String, Object> searchFriendsSpecific(String keyword, int page, int size, String username) {
        // Lấy thông tin người đang thực hiện tìm kiếm
        Account currentUser = accountService.getAccountEntityByUsername(username);
        if (currentUser == null) {
            Map<String, Object> errorRes = new HashMap<>();
            errorRes.put("status", 0);
            return errorRes;
        }

        // Tìm tất cả người dùng khớp từ khóa
        List<Account> accounts = accountService.findByKeyword(keyword);
        List<Map<String, Object>> friendList = new ArrayList<>();

        for (Account account : accounts) {
            if (account.getAccountId().equals(currentUser.getAccountId())) {
                continue;
            }

            // Tìm Conversation giữa CurrentUser và Account này để lấy tin nhắn cuối
            Conversation privateConv = conversationRepository.findPrivateConversationBetween(
                    currentUser.getAccountId(), account.getAccountId());
            List<String> fileUrls = new ArrayList<>();
            List<String> imageUrls = new ArrayList<>();
            Message lastMsg = null;
            if (privateConv != null) {
                lastMsg = messageRepository.findTopByConversation_ConversationIdOrderByCreatedAtDesc(
                        privateConv.getConversationId());
                if (lastMsg != null) {
                    // Truy vấn tất cả attachment của tin nhắn này
                    List<Attachment> attachments = attachmentRepository.findByMessage(lastMsg);

                    for (Attachment att : attachments) {
                        // Phân loại dựa trên fileType (image/... hoặc khác)
                        if (att.getFileType() != null && att.getFileType().startsWith("image/")) {
                            imageUrls.add(att.getFileUrl());
                        } else {
                            fileUrls.add(att.getFileUrl());

                        }
                    }
                }
            }

                // Tạo Map object cho từng friend
                Map<String, Object> friendItem = new HashMap<>();
                friendItem.put("Content", lastMsg != null ? lastMsg.getContent() : "");
                friendItem.put("Files", fileUrls.isEmpty() ? null : fileUrls);
                friendItem.put("Images", imageUrls.isEmpty() ? null : imageUrls);
                friendItem.put("CreatedAt", lastMsg != null ? lastMsg.getCreatedAt() : null);
                friendItem.put("FriendID", account.getAccountId());
                friendItem.put("FullName", account.getDisplayName());
                friendItem.put("Username", account.getUsername());
                friendItem.put("unreadCount", 0);
                friendItem.put("UpdateAtUser", LocalDateTime.now());

                friendList.add(friendItem);
            }

            // Sắp xếp theo thời gian tin nhắn mới nhất
            friendList.sort((a, b) -> {
                java.time.LocalDateTime timeA = (java.time.LocalDateTime) a.get("CreatedAt");
                java.time.LocalDateTime timeB = (java.time.LocalDateTime) b.get("CreatedAt");
                if (timeA == null) return 1;
                if (timeB == null) return -1;
                return timeB.compareTo(timeA);
            });

            // Thực hiện phân trang
            int totalElements = friendList.size();
            int start = Math.min(page * size, totalElements);
            int end = Math.min(start + size, totalElements);
            List<Map<String, Object>> pagedResults = friendList.subList(start, end);


            Map<String, Object> response = new HashMap<>();
            response.put("status", 1);

            Map<String, Object> data = new HashMap<>();
            data.put("friends", pagedResults);
            data.put("totalElements", totalElements);
            response.put("data", data);

            return response;
        }
    }

