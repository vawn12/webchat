package com.bkav.webchat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;


import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(indexName = "messages")
public class MessageDocument {

    @Id
    private Integer messageId; // Dùng chung ID với bảng Message của Postgres

    @Field(type = FieldType.Text, name = "content")
    private String content;

    @Field(type = FieldType.Long, name = "conversation_id")
    private Integer conversationId;

    @Field(type = FieldType.Long, name = "sender_id")
    private Integer senderId;

    @Field(type = FieldType.Date, name = "created_at", format = DateFormat.date_optional_time)
    private Instant createdAt;

    @Field(type = FieldType.Keyword, name = "message_type")
    private String messageType;
}
