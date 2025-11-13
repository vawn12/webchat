package com.bkav.webchat.entity;

import com.bkav.webchat.enumtype.ConversationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "conversation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"participants"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // SERIAL
    @Column(name = "conversation_id")
    private Integer conversationId;

    @Column(name = "name", length = 100)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ConversationType type ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Account createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true,fetch = FetchType.LAZY)
    private Set<Participants> participants = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "jsonb")
    private String settings;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

