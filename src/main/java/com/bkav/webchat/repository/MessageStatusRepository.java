package com.bkav.webchat.repository;

import com.bkav.webchat.entity.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.bkav.webchat.enumtype.Message_Status;
import java.util.List;

public interface MessageStatusRepository extends JpaRepository<MessageStatus, Integer> {
    @Query("SELECT COUNT(ms) FROM MessageStatus ms " +
            "WHERE ms.account.accountId = :accountId " +
            "AND ms.message.conversation.conversationId = :conversationId " +
            "AND ms.status <> 'read'")
    long countUnreadMessages(@Param("accountId") Integer accountId,
                             @Param("conversationId") Integer conversationId);

    // 2. Tìm danh sách các status cần update lên READ
    @Query("SELECT ms FROM MessageStatus ms " +
            "WHERE ms.account.accountId = :accountId " +
            "AND ms.message.conversation.conversationId = :conversationId " +
            "AND ms.status <> 'read'")
    List<MessageStatus> findUnreadStatuses(@Param("accountId") Integer accountId,
                                           @Param("conversationId") Integer conversationId);
}
