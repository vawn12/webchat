package com.bkav.webchat.repository;

import com.bkav.webchat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer>{
    @Query("""
        SELECT m FROM Message m
        WHERE m.conversation.conversationId = :conversationId
        ORDER BY m.createdAt DESC
        LIMIT 1
    """)
    Message findLatestMessage(@Param("conversationId") Integer conversationId);

    Message findTopByConversation_ConversationIdOrderByCreatedAtDesc(Integer conversationId);
}
