package com.bkav.webchat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name="forgot_password")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForgotPassword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @OneToOne
    @JoinColumn(name = "Account_Id", nullable = false)
    private Account account;

    @Column(nullable = false, unique = true, length = 255)
    private Integer token;

    @Column(name = "Expires_At", nullable = false)
    private Date expiryDate;
}
