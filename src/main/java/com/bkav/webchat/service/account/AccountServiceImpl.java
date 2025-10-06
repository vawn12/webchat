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
    public Account convertToEntity(AccountDTO dto) {
        // password sẽ được hash ở đây trước khi lưu vào entity
        return Account.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .displayName(dto.getDisplayName())
                .status(Account_status.OFFLINE)
                .build();

    }
    @Override
    public AccountDTO register(AccountDTO dto, String rawPassword) {
        Account entity = convertToEntity(dto);
        Account saved = accountRepository.save(entity);
        return convertToDTO(saved);
    }

    @Override
    public AccountDTO findByUsername(String username) {
        return accountRepository.findByUsername(username)
                .map(this::convertToDTO)
                .orElse(null);
    }
    @Override
    public AccountDTO findAccountByEmail(String email){
        return accountRepository.findByEmail(email)
                .map(this::convertToDTO)
                .orElse(null);
    }
    @Override
    public AccountDTO login(String username, String rawPassword) {
        return accountRepository.findByUsername(username)
                .filter(acc -> passwordEncoder.matches(rawPassword, acc.getPasswordHash()))
                .map(acc -> {
                    // update trạng thái online nếu muốn
                    acc.setStatus(Account_status.ONLINE);
                    accountRepository.save(acc);
                    return convertToDTO(acc);
                })
                .orElse(null);
    }
    @Override
    public AccountDTO save(AccountDTO dto){
        Account entity = convertToEntity(dto);
        Account saved = accountRepository.save(entity);
        return convertToDTO(saved);
    }
}
