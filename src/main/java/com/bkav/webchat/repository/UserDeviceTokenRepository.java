package com.bkav.webchat.repository;

import com.bkav.webchat.entity.UserDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserDeviceTokenRepository extends JpaRepository<UserDeviceToken, Integer> {
    List<UserDeviceToken> findByAccount_AccountId(Integer accountId);
    void deleteByToken(String token);
    boolean existsByToken(String token);
    Optional<UserDeviceToken> findByToken(String token);
}
