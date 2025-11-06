package com.bkav.webchat.repository;

import com.bkav.webchat.entity.Account;
import com.bkav.webchat.entity.Conversation;
import com.bkav.webchat.entity.Participants;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface ParticipationRepository extends JpaRepository<Participants, Long> {
    List<Participants> findByConversation_ConversationId(Long conversationId);
    Optional<Participants> findByConversationAndAccount(Conversation conversation, Account account);
    List<Participants> findAllByConversation(Conversation conversation);
}
