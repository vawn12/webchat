package com.bkav.webchat.service.verifytoken;

import com.bkav.webchat.dto.VerifyTokenDTO;
import com.bkav.webchat.entity.VerifyToken;

public interface VerifyTokenService {
    VerifyTokenDTO create(VerifyTokenDTO dto);

    VerifyTokenDTO update(VerifyTokenDTO dto);

    void delete(Integer id);

    VerifyTokenDTO getById(Integer id);

    VerifyTokenDTO getTokenByEmail(String email);

    void deleteTokenByEmail(String email);

    VerifyTokenDTO findByToken(String token);

    VerifyTokenDTO toDTO(VerifyToken entity);

    VerifyToken toEntity(VerifyTokenDTO dto);
}
