package com.bkav.webchat.service.verifytoken;

import com.bkav.webchat.dto.ForgotPasswordDTO;
import com.bkav.webchat.entity.Account;
import com.bkav.webchat.entity.ForgotPassword;
import com.bkav.webchat.repository.AccountRepository;
import com.bkav.webchat.repository.ForgotPasswordRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
@Service
public class ForgotService {
    @Autowired
    private ForgotPasswordRepository forgotPasswordRepository;
    @Autowired
    private AccountRepository accountRepository;

    public ForgotPasswordDTO toDTO(ForgotPassword entity) {
        return ForgotPasswordDTO.builder()
                .id(entity.getId())
                .token(entity.getToken())
                .account(entity.getAccount().getUsername())
                .expiryDate(entity.getExpiryDate())
                .build();
    }

    public ForgotPassword ToEntity(ForgotPasswordDTO dto) {
        Account account = accountRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Account not found with ID: " + dto.getId()));

        return ForgotPassword.builder()
                .id(dto.getId())
                .token(dto.getToken())
                .account(account)
                .expiryDate(dto.getExpiryDate())
                .build();
    }

    @Transactional
    public ForgotPasswordDTO createForgotPassword(ForgotPasswordDTO dto) {
        Account account = accountRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Account not found with ID: " + dto.getAccountId()));

        Optional<ForgotPassword> existing = forgotPasswordRepository.findByAccount(account);
        if (existing.isPresent()) {
            ForgotPassword forgot = existing.get();
            forgot.setToken(dto.getToken());
            forgot.setExpiryDate(dto.getExpiryDate());
            return toDTO(forgotPasswordRepository.save(forgot));
        }
        ForgotPassword newForgot = new ForgotPassword();
        newForgot.setAccount(account);
        newForgot.setToken(dto.getToken());
        newForgot.setExpiryDate(dto.getExpiryDate());
        return toDTO(forgotPasswordRepository.save(newForgot));
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
        existing.setToken(dto.getToken());
        existing.setExpiryDate(dto.getExpiryDate());
        existing.setAccount(accountRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Account not found with ID: " + dto.getId())));

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
