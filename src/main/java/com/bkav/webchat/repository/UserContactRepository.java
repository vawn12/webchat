package com.bkav.webchat.repository;

import com.bkav.webchat.entity.Account;
import com.bkav.webchat.entity.UserContact;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserContactRepository extends JpaRepository<UserContact,Integer> {
    Page<UserContact> findAll(Pageable pageable);
    @Query("SELECT u FROM UserContact u WHERE u.status = com.bkav.webchat.enumtype.ContactStatus.accepted")
    List<UserContact> findAllAccepted();



}
