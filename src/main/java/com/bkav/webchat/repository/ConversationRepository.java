package com.bkav.webchat.repository;

import com.bkav.webchat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation,Integer> {

}
