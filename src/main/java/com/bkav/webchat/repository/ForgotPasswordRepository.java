package com.bkav.webchat.repository;

import com.bkav.webchat.entity.Account;
import com.bkav.webchat.entity.ForgotPassword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ForgotPasswordRepository extends JpaRepository<ForgotPassword,Integer> {
    Optional<ForgotPassword> findByAccount(Account account);

}
