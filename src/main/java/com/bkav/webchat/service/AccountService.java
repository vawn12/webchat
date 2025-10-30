package com.bkav.webchat.service;

import com.bkav.webchat.dto.m.AccountDTO;
import com.bkav.webchat.entity.Account;

import java.util.List;

public interface AccountService {
    AccountDTO register(AccountDTO dto, String rawPassword);
    AccountDTO findByUsername(String username);
    AccountDTO findAccountByEmail(String email);
    AccountDTO login(String username, String rawPassword);
    AccountDTO save(AccountDTO dto);
    AccountDTO convertToDTO(Account account);
    Account convertToEntity(AccountDTO dto);
    List<Account> getAllAccount ();
}
