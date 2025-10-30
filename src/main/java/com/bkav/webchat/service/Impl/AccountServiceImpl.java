package com.bkav.webchat.service.Impl;
import com.bkav.webchat.cache.RedisService;
import com.bkav.webchat.enumtype.Account_status;
import com.bkav.webchat.dto.m.AccountDTO;
import com.bkav.webchat.entity.Account;
import com.bkav.webchat.repository.AccountRepository;
import com.bkav.webchat.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private BCryptPasswordEncoder encoder  = new BCryptPasswordEncoder();
    @Autowired
    private RedisService redisService;

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

    public Account convertToEntity(AccountDTO dto) {
        return Account.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .displayName(dto.getDisplayName())
                .status(Account_status.offline)
                .build();
    }
    @Override
    public AccountDTO register(AccountDTO dto, String rawPassword) {
        if (dto == null || rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Account info or password is missing");
        }

        // Convert DTO → Entity
        Account entity = convertToEntity(dto);
        // mật khẩu được mã hóa
        entity.setPasswordHash(encoder.encode(rawPassword));
        if (entity.getStatus() == null) {
            entity.setStatus(Account_status.offline);
        }
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

    public AccountDTO login(String username, String password) {
        String cacheKey = "account:" + username;

        // Không kiểm tra password trong cache
        AccountDTO cachedAccount = (AccountDTO) redisService.get(cacheKey);
        if (cachedAccount != null) {
            // Chỉ kiểm tra nếu Redis cache vẫn hợp lệ
            Account account = accountRepository.findByUsername(username).orElse(null);
            if (account != null && encoder.matches(password, account.getPasswordHash())) {
                System.out.println("Cache hit from Redis!");
                return cachedAccount;
            } else {
                return null;
            }
        }

        // Nếu không có cache → kiểm tra trong DB
        Account account = accountRepository.findByUsername(username).orElse(null);
        if (account == null || !encoder.matches(password, account.getPasswordHash())) {
            return null;
        }

        // Chuyển sang DTO (không chứa password)
        AccountDTO dto = AccountDTO.builder()
                .accountId(account.getAccountId())
                .username(account.getUsername())
                .email(account.getEmail())
                .displayName(account.getDisplayName())
                .status(account.getStatus())
                .build();

        // Lưu cache trong Redis 30 phút
        redisService.save(cacheKey, dto, 30);

        return dto;
    }

    @Override
    public AccountDTO save(AccountDTO dto){
        Account entity = convertToEntity(dto);
        Account saved = accountRepository.save(entity);
        return convertToDTO(saved);
    }

    @Override
    public List<Account> getAllAccount (){
        return accountRepository.findAll();
    }
}
