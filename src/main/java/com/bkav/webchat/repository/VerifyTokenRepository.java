package com.bkav.webchat.repository;

import com.bkav.webchat.entity.VerifyToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerifyTokenRepository extends JpaRepository<VerifyToken,Integer> {
    public Optional<VerifyToken> findByEmail(String email);

    public void deleteByEmail(String email);

    public Optional<VerifyToken> findByToken(String token);
}
