//package com.bkav.webchat.repository;
//
//import com.bkav.webchat.dto.ChatListDTO;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.repository.query.Param;
//
//import java.util.List;
//
//public interface ChatListRepository extends JpaRepository<Object, Integer> {
//    @Query(value = """
//        WITH user_convs AS (
//            SELECT p.conversation_id
//            FROM participants p
//            WHERE p.account_id = :userId
//        ),
//        last_msg AS (
//            SELECT DISTINCT ON (m.conversation_id)
//                m.conversation_id,
//                m.content,
//                m.created_at,
//                m.is_read
//            FROM message m
//            JOIN user_convs uc ON uc.conversation_id = m.conversation_id
//            ORDER BY m.conversation_id, m.created_at DESC
//        ),
//        unread_count AS (
//            SELECT m.conversation_id, COUNT(*) AS unread
//            FROM message m
//            JOIN user_convs uc ON uc.conversation_id = m.conversation_id
//            WHERE m.is_read = FALSE AND m.sender_id <> :userId
//            GROUP BY m.conversation_id
//        )
//        SELECT
//            c.conversation_id AS conversationId,
//            c.name AS title,
//            c.type AS type,
//            COALESCE(lm.content, '') AS lastMessagePreview,
//            lm.created_at AS lastMessageTime,
//            COALESCE(u.unread, 0) AS unreadCount
//        FROM conversation c
//        LEFT JOIN last_msg lm ON lm.conversation_id = c.conversation_id
//        LEFT JOIN unread_count u ON u.conversation_id = c.conversation_id
//        JOIN participants p ON p.conversation_id = c.conversation_id
//        WHERE p.account_id = :userId
//        ORDER BY lm.created_at DESC NULLS LAST
//    """, nativeQuery = true)
//    List<Object[]> findChatListByUserId(@Param("userId") Long userId);
//}
