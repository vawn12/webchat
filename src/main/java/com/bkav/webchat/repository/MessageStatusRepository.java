package com.bkav.webchat.repository;

import com.bkav.webchat.entity.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageStatusRepository extends JpaRepository<MessageStatus, Integer> {
}
