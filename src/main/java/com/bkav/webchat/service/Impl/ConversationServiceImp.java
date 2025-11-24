package com.bkav.webchat.service.Impl;

import com.bkav.webchat.dto.ContactResponseDTO;
import com.bkav.webchat.dto.m.ConversationDTO;
import com.bkav.webchat.dto.m.ParticipantDTO;
import com.bkav.webchat.entity.*;
import com.bkav.webchat.enumtype.ConversationType;
import com.bkav.webchat.enumtype.ParticipantRole;
import com.bkav.webchat.repository.*;
import com.bkav.webchat.security.JwtService;
import com.bkav.webchat.service.AccountService;
import com.bkav.webchat.service.ConversationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConversationServiceImp implements ConversationService {
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private AccountService accountService;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private ParticipationRepository participantRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private UserContactRepository userContactRepository;


    public List<ConversationDTO> getConversationsByType(String authorizationHeader, String type) {
        Account account = extractAccount(authorizationHeader);
        if (account == null) return List.of();

        List<Conversation> conversations = conversationRepository.findAllByAccountIdAndType(account.getAccountId(), type);
        return conversations.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private Account extractAccount(String header) {
        if (header == null || !header.startsWith("Bearer")) return null;
        String token = header.substring(7);
        String username = jwtService.extractUsername(token);
        return accountRepository.findByUsername(username).orElse(null);
    }

    private ConversationDTO mapToDTO(Conversation c) {
        ConversationDTO dto = new ConversationDTO();
        dto.setConversationId(c.getConversationId());
        dto.setName(c.getName());
        dto.setType(c.getType());
        dto.setCreatedBy(c.getCreatedBy().getDisplayName());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setParticipants(
                new ArrayList<>(c.getParticipants()).stream()
                        .map(p -> new ParticipantDTO(
                                p.getAccount().getAccountId(),
                                p.getAccount().getDisplayName(),
                                p.getAccount().getAvatarUrl(),
                                p.getRole()))
                        .collect(Collectors.toList())
        );
        return dto;
    }
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
    @Override
    @Transactional
    public ConversationDTO createGroupConversation(String authorizationHeader, String name, List<Integer> participantIds) {
        Account creator = extractAccount(authorizationHeader);
        if (creator == null) {
            throw new RuntimeException("Unauthorized: Invalid token");
        }

        // Tạo conversation
        Conversation conversation = new Conversation();
        conversation.setName(name);
        conversation.setType(ConversationType.GROUP);
        conversation.setCreatedBy(creator);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation = conversationRepository.save(conversation);

        //thêm người tạo nhóm admin
        Participants admin = Participants.builder()
                .conversation(conversation)
                .account(creator)
                .role(ParticipantRole.admin)
                .build();

        participantRepository.save(admin);

        //điểu kiện để tạo nhóm
        if (participantIds == null || participantIds.size() < 2) {
            throw new RuntimeException("Nhóm phải có ít nhất 3 thành viên (bao gồm người tạo nhóm).");
        }

        // lấy tất cả account để thêm
        List<Account> accounts = accountRepository.findAllById(participantIds);

        // Tạo list Participants
        Conversation finalConversation = conversation;
        List<Participants> members = accounts.stream()
                .map(acc -> Participants.builder()
                        .conversation(finalConversation)
                        .account(acc)
                        .role(ParticipantRole.member)
                        .build())
                .toList();
        participantRepository.saveAll(members);

        // Load lại conversation kèm participants để trả DTO
        Conversation fullData = conversationRepository
                .findByIdWithParticipants(conversation.getConversationId())
                .orElseThrow(() -> new RuntimeException("đoạn chat không tìm thấy sau khi lưu"));

        return mapToDTO(fullData);
    }

    //thêm thành viên mới vào nhóm chát
    @Transactional
    public ConversationDTO addMembersToGroup(String authorizationHeader,
                                             Integer conversationId,
                                             List<Integer> newMemberIds) {
        Account requester = extractAccount(authorizationHeader);
        if (requester == null) {
            throw new RuntimeException("Unauthorized");
        }

        // Lấy conversation
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (!ConversationType.GROUP.equals(conversation.getType())) {
            throw new RuntimeException("Chỉ nhóm mới được thêm thành viên");
        }

        // Kiểm tra requester có trong nhóm hay không
        Participants requesterParticipant = participantRepository.findParticipant(
                        conversationId,
                        requester.getAccountId()
                )
                .orElseThrow(() -> new RuntimeException("Bạn không nằm trong nhóm này"));

        // Kiểm tra quyền
        String role = requesterParticipant.getRole().name();
        if (!role.equalsIgnoreCase("admin") && !role.equalsIgnoreCase("member")) {
            throw new RuntimeException("Bạn không có quyền thêm thành viên");
        }

        // Load account từ danh sách ID
        List<Account> newMembers = accountRepository.findAllById(newMemberIds);
        if (newMembers.isEmpty()) {
            throw new RuntimeException("Không có tài khoản hợp lệ nào");
        }

        // Lấy ID đã tồn tại trong group nhưng chỉ trong newMemberIds
        List<Integer> existingIds = participantRepository.findExistingAccountIds(
                conversationId,
                newMemberIds
        );
        Set<Integer> existingIdSet = new HashSet<>(existingIds);

        // Lọc ra những account chưa ở trong group
        List<Participants> newParticipants = newMembers.stream()
                .filter(acc -> !existingIdSet.contains(acc.getAccountId()))
                .map(acc -> Participants.builder()
                        .conversation(conversation)
                        .account(acc)
                        .role(ParticipantRole.member)
                        .build())
                .toList();

        // Thêm vào DB
        if (!newParticipants.isEmpty()) {
            participantRepository.saveAll(newParticipants);
        }

        // Load lại conversation và participants
        Conversation updated = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new RuntimeException("lỗi tải lại đoạn hội thoai"));

        return mapToDTO(updated);
    }

    //lấy tất cả cuộc trò chuyện
    public Page<ContactResponseDTO> getAllConversation(int page, int size) {
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
// Trong file ConversationServiceImp.java

    @Override
    @Transactional(readOnly = true)
    public ConversationDTO getConversationDetails(String authorizationHeader, Integer conversationId) {
        Account requester = extractAccount(authorizationHeader);
        if (requester == null) {
            throw new RuntimeException("Unauthorized: Invalid token");
        }

        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId) // <-- Sửa ở đây
                .orElseThrow(() -> new RuntimeException("Conversation not found"));


        // Kiểm tra xem người dùng có phải là thành viên của nhóm này không
        participantRepository.findByConversationAndAccount(conversation, requester)
                .orElseThrow(() -> new RuntimeException("Access Denied: Bạn không phải là thành viên của cuộc trò chuyện này"));

        System.out.println("Participants size = " + conversation.getParticipants().size());
        return mapToDTO(conversation);
    }


    @Override
    @Transactional
    public void leaveGroup(String authorizationHeader, Integer conversationId) {
        Account requester = extractAccount(authorizationHeader);
        if (requester == null) {
            throw new RuntimeException("Unauthorized: Invalid token");
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // Chỉ cho phép rời nhóm, không cho rời chat riêng tư
        if (conversation.getType() != ConversationType.GROUP) {
            throw new RuntimeException("Chỉ có thể rời khỏi cuộc trò chuyện nhóm.");
        }

        // Tìm bản ghi tham gia của người dùng
        Participants participant = participantRepository
                .findByConversationAndAccount(conversation, requester)
                .orElseThrow(() -> new RuntimeException("Bạn không phải là thành viên của nhóm này"));

        // Lấy danh sách tất cả thành viên *trước khi* rời
        List<Participants> allParticipants = participantRepository.findAllByConversation(conversation);

        if (allParticipants.size() <= 1) {
            // Nếu đây là người cuối cùng, xóa luôn cả cuộc trò chuyện
            conversationRepository.delete(conversation);
        } else {
            // nếu người rời là admin cuối cùng
            boolean isRequesterAdmin = participant.getRole() == ParticipantRole.admin;
            long totalAdmins = allParticipants.stream()
                    .filter(p -> p.getRole() == ParticipantRole.admin)
                    .count();

            // Xóa người này khỏi nhóm
            participantRepository.delete(participant);

            if (isRequesterAdmin && totalAdmins == 1) {
                // Lấy thành viên bất kỳ còn lại làm admin (sau khi đã xóa người trên)
                Participants newAdmin = participantRepository.findAllByConversation(conversation)
                        .stream()
                        .findFirst()
                        .orElse(null);

                if (newAdmin != null) {
                    newAdmin.setRole(ParticipantRole.admin);
                    participantRepository.save(newAdmin);
                }
            }
        }
    }

    @Transactional
    public ConversationDTO createPrivateConversation(String authorizationHeader, Integer friendId) {
        Account requester = extractAccount(authorizationHeader);
        if (requester == null) {
            throw new RuntimeException("Unauthorized: Invalid token");
        }

        if (Objects.equals(requester.getAccountId(), friendId)) {
            throw new RuntimeException("Không thể tạo đoạn chat với chính bạn.");
        }

        Account friend = accountRepository.findById(friendId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        // Kiểm tra đoạn hội thoại đã tồn tại chưa
        Conversation existing = conversationRepository.findPrivateConversationBetween(
                requester.getAccountId(), friendId);

        if (existing != null) {
            // Load đầy đủ để trả về DTO
            Conversation full = conversationRepository
                    .findByIdWithParticipants(existing.getConversationId())
                    .orElseThrow(() -> new RuntimeException("Không tải được đoạn chat private"));
            return mapToDTO(full);
        }

        // Tạo mới cuộc trò chuyện Private
        Conversation conversation = new Conversation();
        conversation.setName(null);
        conversation.setType(ConversationType.PRIVATE);
        conversation.setCreatedBy(requester);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation = conversationRepository.save(conversation);

        // Thêm 2 participants
        Participants p1 = Participants.builder()
                .conversation(conversation)
                .account(requester)
                .role(ParticipantRole.member)
                .build();

        Participants p2 = Participants.builder()
                .conversation(conversation)
                .account(friend)
                .role(ParticipantRole.member)
                .build();

        participantRepository.save(p1);
        participantRepository.save(p2);

        // Load lại dữ liệu đầy đủ
        Conversation fullData = conversationRepository
                .findByIdWithParticipants(conversation.getConversationId())
                .orElseThrow(() -> new RuntimeException("Lỗi tải lại private chat"));

        return mapToDTO(fullData);
    }


}


