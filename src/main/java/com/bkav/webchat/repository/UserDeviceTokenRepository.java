package com.bkav.webchat.repository;

import com.bkav.webchat.entity.UserDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserDeviceTokenRepository extends JpaRepository<UserDeviceToken, Integer> {
    List<UserDeviceToken> findByAccount_AccountId(Integer accountId);

    @Transactional
    void deleteByToken(String token);

    boolean existsByToken(String token);

    Optional<UserDeviceToken> findByToken(String token);

    List<UserDeviceToken> findAllByAccount_AccountIdIn(List<Integer> accountIds);
}
