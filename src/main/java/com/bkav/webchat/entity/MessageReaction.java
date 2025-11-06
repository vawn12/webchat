package com.bkav.webchat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Table (name = "Message_Reaction")
@Entity

public class MessageReaction {

    @EmbeddedId
    private MessageReactionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("messageId")
    @JoinColumn(name = "Message_Id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("accountId")
    @JoinColumn(name = "Account_Id", nullable = false)
    private Account account;

    @Column(name = "Reaction_Type", nullable = false, length = 20)
    private String reactionType;

    @Column(name = "Created_At")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class MessageReactionId implements Serializable {
        @Column(name = "Message_Id")
        private Integer messageId;

        @Column(name = "Account_Id")
        private Integer accountId;
    }
}
