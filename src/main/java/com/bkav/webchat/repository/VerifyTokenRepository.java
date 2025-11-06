package com.bkav.webchat.repository;

import com.bkav.webchat.entity.VerifyToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerifyTokenRepository extends JpaRepository<VerifyToken,Integer> {
     Optional<VerifyToken> findByEmail(String email);

     void deleteByEmail(String email);

     Optional<VerifyToken> findByToken(String token);
}
