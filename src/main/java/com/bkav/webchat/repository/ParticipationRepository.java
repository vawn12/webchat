package com.bkav.webchat.repository;

import com.bkav.webchat.entity.Account;
import com.bkav.webchat.entity.Conversation;
import com.bkav.webchat.entity.Participants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface ParticipationRepository extends JpaRepository<Participants, Long> {
    List<Participants> findByConversation_ConversationId(Long conversationId);
    Optional<Participants> findByConversationAndAccount(Conversation conversation, Account account);
    List<Participants> findAllByConversation(Conversation conversation);
    List<Participants> findAllByAccount_AccountId(Integer accountId);
    @Query("""
        select p.account.accountId
        from Participants p
        where p.conversation.conversationId = :conversationId
          and p.account.accountId in :accountIds
    """)
    List<Integer> findExistingAccountIds(
            @Param("conversationId") Integer conversationId,
            @Param("accountIds") List<Integer> accountIds
    );

    @Query("""
        select p
        from Participants p
        where p.conversation.conversationId = :conversationId
          and p.account.accountId = :accountId
    """)
    Optional<Participants> findParticipant(
            @Param("conversationId") Integer conversationId,
            @Param("accountId") Integer accountId
    );
}
