package com.bkav.webchat.repository;

import com.bkav.webchat.entity.Participation;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ParticipationRepository extends JpaRepository<Participation, Long> {
}
