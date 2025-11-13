package com.bkav.webchat.repository;

import com.bkav.webchat.entity.Conversation;
import com.bkav.webchat.enumtype.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation,Integer> {

    @Query("""
        SELECT DISTINCT c FROM Conversation c 
        JOIN c.participants p 
        WHERE p.account.accountId = :accountId
        """)
    List<Conversation> findAllByAccountId(@Param("accountId") Integer accountId);

    @Query("""
        SELECT DISTINCT c FROM Conversation c 
        JOIN c.participants p 
        WHERE p.account.accountId = :accountId AND c.type = :type
        """)
    List<Conversation> findAllByAccountIdAndType(@Param("accountId") Integer accountId, @Param("type") String type);


    @Query("SELECT c FROM Conversation c WHERE c.type = :type")
    List<Conversation> findAllByType(ConversationType type);
    @Query("""
        SELECT c FROM Conversation c 
        JOIN c.participants p1 
        JOIN c.participants p2 
        WHERE c.type = com.bkav.webchat.enumtype.ConversationType.PRIVATE
          AND p1.account.accountId = :user1Id
          AND p2.account.accountId = :user2Id
    """)
    Conversation findPrivateConversationBetween(@Param("user1Id") Integer user1Id,
                                                @Param("user2Id") Integer user2Id);
    List<Conversation> findAllByTypeAndNameContainingIgnoreCase(ConversationType type, String name);

    @Query("""
       SELECT DISTINCT c FROM Conversation c
       LEFT JOIN FETCH c.participants p
       LEFT JOIN FETCH p.account
       WHERE c.conversationId = :id
       """)
    Optional<Conversation> findByIdWithParticipants(@Param("id") Integer id);


}
