package com.bkav.webchat.repository;

import com.bkav.webchat.entity.Attachment;
import com.bkav.webchat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByMessage(Message message);
}
