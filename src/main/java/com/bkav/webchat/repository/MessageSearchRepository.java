package com.bkav.webchat.repository;

import com.bkav.webchat.entity.MessageDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface MessageSearchRepository extends ElasticsearchRepository<MessageDocument, Integer> {
    List<MessageDocument> findByContentContainingAndConversationIdIn(String content, List<Integer> conversationIds);
}
