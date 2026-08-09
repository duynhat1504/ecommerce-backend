package com.duynhat.ecommerce_backend.modules.auth.impl;

import com.duynhat.ecommerce_backend.modules.auth.RefreshTokenCompromiseService;
import com.duynhat.ecommerce_backend.modules.auth.RefreshTokenRepository;
import com.duynhat.ecommerce_backend.modules.auth.entity.RefreshToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RefreshTokenCompromiseServiceImpl implements RefreshTokenCompromiseService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenCompromiseServiceImpl(
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.refreshTokenRepository =
                refreshTokenRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compromiseSession(UUID sessionId) {

        var activeTokens = refreshTokenRepository
                        .findAllBySessionIdAndRevokedAtIsNull(sessionId);

        activeTokens.forEach(RefreshToken::revoke);

        refreshTokenRepository.saveAll(activeTokens);
    }
}
