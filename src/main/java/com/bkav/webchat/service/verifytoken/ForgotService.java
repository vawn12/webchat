package com.bkav.webchat.service.verifytoken;

import com.bkav.webchat.dto.ForgotPasswordDTO;
import com.bkav.webchat.entity.Account;
import com.bkav.webchat.entity.ForgotPassword;
import com.bkav.webchat.repository.AccountRepository;
import com.bkav.webchat.repository.ForgotPasswordRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ForgotService {
    @Autowired
    private ForgotPasswordRepository forgotPasswordRepository;
    @Autowired
    private AccountRepository accountRepository;

    public ForgotPasswordDTO toDTO(ForgotPassword entity) {
        return ForgotPasswordDTO.builder()
                .id(entity.getId())
                .token(entity.getToken())
                .account(entity.getAccount().getAccountId())
                .expiryDate(entity.getExpiryDate())
                .build();
    }

    public ForgotPassword ToEntity(ForgotPasswordDTO dto) {
        Account account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found with ID: " + dto.getAccountId()));

        return ForgotPassword.builder()
                .id(dto.getId())
                .otp(dto.getOtp())
                .account(account)
                .expiryDate(dto.getExpiryDate())
                .build();
    }

    @Transactional
    public ForgotPasswordDTO createForgotPassword(ForgotPasswordDTO dto) {
        ForgotPassword entity = ToEntity(dto);
        ForgotPassword saved = forgotPasswordRepository.save(entity);
        return toDTO(saved);
    }

    @Transactional
    public ForgotPasswordDTO getForgotPasswordById(Integer id) {
        ForgotPassword entity = forgotPasswordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ForgotPassword not found with ID: " + id));
        return toDTO(entity);
    }

    @Transactional
    public List<ForgotPasswordDTO> getAllForgotPasswords() {
        return forgotPasswordRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ForgotPasswordDTO updateForgotPassword(Integer id, ForgotPasswordDTO dto) {
        ForgotPassword existing = forgotPasswordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ForgotPassword not found with ID: " + id));

        // Cập nhật dữ liệu
        existing.setOtp(dto.getOtp());
        existing.setExpiryDate(dto.getExpiryDate());
        existing.setAccount(accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found with ID: " + dto.getAccountId())));

        ForgotPassword updated = forgotPasswordRepository.save(existing);
        return toDTO(updated);
    }

    @Transactional
    public void deleteForgotPassword(Integer id) {
        if (!forgotPasswordRepository.existsById(id)) {
            throw new RuntimeException("ForgotPassword not found with ID: " + id);
        }
        forgotPasswordRepository.deleteById(id);
    }

    @Transactional
    public ForgotPasswordDTO findForgotPasswordByAccountId(Integer accountId) {
        Account account = accountRepository.findById(accountId).get();
        Optional<ForgotPassword> entity = forgotPasswordRepository.findByAccount(account);
        if (entity.isPresent()) {
            ForgotPassword forgotPassword = entity.get();
            return toDTO(forgotPassword);
        } else
            return null;
    }
}
