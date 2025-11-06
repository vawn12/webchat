package com.bkav.webchat.repository;

import com.bkav.webchat.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {
    Optional<MessageReaction> findByMessage_MessageIdAndAccount_AccountId(Integer messageId, Integer accountId);
}
