package com.bkav.webchat.service.account;
import com.bkav.webchat.enumtype.Account_status;
import com.bkav.webchat.dto.AccountDTO;
import com.bkav.webchat.entity.Account;
import com.bkav.webchat.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceImpl implements AccountService {
    @Autowired
    private AccountRepository accountRepository;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AccountDTO convertToDTO(Account account) {
        return AccountDTO.builder()
                .accountId(account.getAccountId())
                .username(account.getUsername())
                .email(account.getEmail())
                .displayName(account.getDisplayName())
                .avatarUrl(account.getAvatarUrl())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .build();
    }

    // Chuyển từ AccountDTO (DTO) sang Account (Entity)
    public Account convertToEntity(AccountDTO creationDTO, String rawPassword) {
        // password sẽ được hash ở đây trước khi lưu vào entity
        return Account.builder()
                .username(creationDTO.getUsername())
                .email(creationDTO.getEmail())
                .displayName(creationDTO.getDisplayName())
                .status(Account_status.OFFLINE)
                .build();

    }
    @Override
    public AccountDTO register(AccountDTO dto, String rawPassword) {
        Account entity = convertToEntity(dto, rawPassword);
        Account saved = accountRepository.save(entity);
        return convertToDTO(saved);
    }

    @Override
    public AccountDTO findByUsername(String username) {
        return accountRepository.findByUsername(username)
                .map(this::convertToDTO)
                .orElse(null);
    }
}
