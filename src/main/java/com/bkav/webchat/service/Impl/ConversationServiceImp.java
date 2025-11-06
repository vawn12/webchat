package com.bkav.webchat.service.Impl;

import com.bkav.webchat.dto.m.ConversationDTO;
import com.bkav.webchat.dto.m.ParticipantDTO;
import com.bkav.webchat.entity.Account;
import com.bkav.webchat.entity.Conversation;
import com.bkav.webchat.entity.Participants;
import com.bkav.webchat.enumtype.ConversationType;
import com.bkav.webchat.enumtype.ParticipantRole;
import com.bkav.webchat.repository.AccountRepository;
import com.bkav.webchat.repository.ConversationRepository;
import com.bkav.webchat.repository.ParticipationRepository;
import com.bkav.webchat.security.JwtService;
import com.bkav.webchat.service.AccountService;
import com.bkav.webchat.service.ConversationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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

        public List<ConversationDTO> getConversationsByToken(String authorizationHeader) {
            Account account = extractAccount(authorizationHeader);
            if (account == null) return List.of();

            List<Conversation> conversations = conversationRepository.findAllByAccountId(account.getAccountId());
            return conversations.stream().map(this::mapToDTO).collect(Collectors.toList());
        }

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
                    c.getParticipants().stream()
                            .map(p -> new ParticipantDTO(
                                    p.getAccount().getAccountId(),
                                    p.getAccount().getDisplayName(),
                                    p.getAccount().getAvatarUrl(),
                                    p.getRole()))
                            .collect(Collectors.toList())
            );
            return dto;
        }
        
    public ConversationDTO createGroupConversation(String authorizationHeader, String name, List<Integer> participantIds) {
        Account creator = extractAccount(authorizationHeader);
        if (creator == null) {
            throw new RuntimeException("Unauthorized: Invalid token");
        }
        //  Tạo cuộc trò chuyện mới
        Conversation conversation = new Conversation();
        conversation.setName(name);
        conversation.setType(ConversationType.GROUP);
        conversation.setCreatedBy(creator);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation = conversationRepository.save(conversation);

        //  Thêm người tạo nhóm (vai trò admin)
        Participants admin = new Participants();
        admin.setConversation(conversation);
        admin.setAccount(creator);
        admin.setRole(ParticipantRole.admin);
        participantRepository.save(admin);

        // Thêm các thành viên còn lại
        for (Integer id : participantIds) {
            Conversation conver = conversation;
            accountRepository.findById(id).ifPresent(acc -> {
                Participants member = new Participants();
                member.setConversation(conver);
                member.setAccount(acc);
                member.setRole(ParticipantRole.member);
                participantRepository.save(member);
            });
        }
       
        return mapToDTO(conversation);
    }
    //thêm thành viên mới vào nhóm chát
    @Transactional
    public ConversationDTO addMembersToGroup(String authorizationHeader, Integer conversationId, List<Integer> newMemberIds) {
        Account requester = extractAccount(authorizationHeader);
        if (requester == null) throw new RuntimeException("Unauthorized");
        //tìm nhưng cuoc tro chuyen
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        //kiểm tra có phải member trong group không
        if (!conversation.getType().name().equalsIgnoreCase("GROUP"))
            throw new RuntimeException("Chỉ nhóm mới được thêm thành viên");

        Participants requesterParticipant = participantRepository
                .findByConversationAndAccount(conversation, requester)
                .orElseThrow(() -> new RuntimeException("Bạn không nằm trong nhóm này"));
        //phân quyền thêm thành viên (tinh năng có thể phát triển)
        String role = requesterParticipant.getRole().name();
        if (!role.equalsIgnoreCase("ADMIN") && !role.equalsIgnoreCase("MEMBER"))
            throw new RuntimeException("Bạn không có quyền thêm thành viên");

        for (Integer id : newMemberIds) {
            Account member = accountRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản ID " + id));

            if (participantRepository.findByConversationAndAccount(conversation, member).isEmpty()) {
                Participants newParticipant = Participants.builder()
                        .conversation(conversation)
                        .account(member)
                        .role(ParticipantRole.member)
                        .build();
                participantRepository.save(newParticipant);
            }
        }

        conversationRepository.flush();

        Conversation updated = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        return mapToDTO(updated);
    }

    }


