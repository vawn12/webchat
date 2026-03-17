package com.bkav.webchat.repository;

import com.bkav.webchat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer>{
    Message findTopByConversation_ConversationIdOrderByCreatedAtDesc(Integer conversationId);
    
    Page<Message> findByConversation_ConversationIdOrderByCreatedAtDesc(Integer conversationId, Pageable pageable);
}
