package com.bkav.webchat.security;

import com.bkav.webchat.entity.Account;
import com.bkav.webchat.enumtype.Account_status;
import com.bkav.webchat.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AccountDetailService implements UserDetailsService {
    @Autowired
    private AccountRepository accountRepository;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        boolean locked = account.getStatus() == Account_status.BANNER;

        return User.builder()
                .username(account.getEmail())
                .password(account.getPasswordHash())
                .accountLocked(locked)
                .roles("USER")  // mặc định tất cả user
                .build();
    }

}
