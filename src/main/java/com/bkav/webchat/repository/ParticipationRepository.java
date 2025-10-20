package com.bkav.webchat.repository;

import com.bkav.webchat.entity.Participation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    List<Participation> findByConversation_ConversationId(Long conversationId);
}
