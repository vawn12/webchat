package com.bkav.webchat.entity;

import com.bkav.webchat.enumtype.ContactStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "user_contact",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_contact_constraint",
                        columnNames = {"owner_id", "contact_user_id"}
                )
        }
)
public class UserContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contact_id")
    private Long contactId;

    // Quan hệ Many-to-One: Nhiều contact entry thuộc về một owner
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Account owner;

    // Quan hệ Many-to-One: Nhiều contact entry trỏ đến một user trong danh bạ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_user_id", nullable = false)
    private Account contactUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ContactStatus status; // Gán giá trị mặc định

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
