package com.bkav.webchat.repository;

import com.bkav.webchat.entity.MessageDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface MessageSearchRepository extends ElasticsearchRepository<MessageDocument, Integer> {
    List<MessageDocument> findByContent(String content);
    @Query("{\"bool\": {\"must\": [{\"match\": {\"content\": \"?0\"}}], \"filter\": [{\"terms\": {\"conversation_id\": ?1}}]}}")
    List<MessageDocument> findByContentAndConversationIds(String content, List<Integer> conversationIds);
    List<MessageDocument> findByContentContainingAndConversationIdIn(String content, List<Integer> conversationIds);
}
