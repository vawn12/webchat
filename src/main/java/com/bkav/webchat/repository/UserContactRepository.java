package com.bkav.webchat.repository;

import com.bkav.webchat.entity.UserContact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserContactRepository extends JpaRepository<UserContact,Integer> {
}
