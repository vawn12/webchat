package com.bkav.webchat.repository;

import com.bkav.webchat.entity.Account;
import com.bkav.webchat.entity.UserContact;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserContactRepository extends JpaRepository<UserContact,Integer> {
    Page<UserContact> findAll(Pageable pageable);
    @Query("SELECT u FROM UserContact u WHERE u.status = com.bkav.webchat.enumtype.ContactStatus.accepted")
    List<UserContact> findAllAccepted();
    @Query("SELECT uc FROM UserContact uc WHERE uc.owner.accountId = :accountId AND uc.status = com.bkav.webchat.enumtype.ContactStatus.accepted")
    List<UserContact> findAllAcceptedByAccountId(@Param("accountId") Integer accountId);
    Optional<UserContact> findByOwnerAndContactUser(Account owner, Account contactUser);
}
