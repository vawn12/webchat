package com.bkav.webchat.service.account;

import com.bkav.webchat.dto.AccountDTO;
import com.bkav.webchat.entity.Account;

public interface AccountService {
    AccountDTO register(AccountDTO dto, String rawPassword);
    AccountDTO findByUsername(String username);

}
