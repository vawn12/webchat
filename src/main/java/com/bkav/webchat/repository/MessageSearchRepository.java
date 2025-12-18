package com.bkav.webchat.repository;

import com.bkav.webchat.entity.MessageDocument;
import com.bkav.webchat.enumtype.Message_Status;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Collection;
import java.util.List;

public interface MessageSearchRepository extends ElasticsearchRepository<MessageDocument, Integer> {
    List<MessageDocument> findByContentContainingAndConversationIdIn(String content, List<Integer> conversationIds);
    List<MessageDocument> findByContentContainingAndConversationIdInAndMessageTypeNot(
            String content, Collection<Integer> conversationId, String messageType
    );
}
