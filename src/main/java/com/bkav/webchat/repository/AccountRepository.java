package com.bkav.webchat.repository;

import com.bkav.webchat.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface AccountRepository extends JpaRepository<Account,Integer> {
//    getAccountById
    Optional<Account> findByUsername(String username);

    Optional<Account> findByEmail(String email);

    @Query("SELECT a FROM Account a WHERE LOWER(a.displayName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Account> findByKeyword(@Param("keyword") String keyword);

    @Query("SELECT a FROM Account a " +
            "WHERE LOWER(a.displayName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Account> searchAccounts(@Param("keyword") String keyword);
    boolean existsByUsername(String username);
    Account findByAccountId(Integer accountId);
    @Query("SELECT a.displayName FROM Account a WHERE a.accountId = :id")
    String findDisplayNameById(@Param("id") Integer id);
}
