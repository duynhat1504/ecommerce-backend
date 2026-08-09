package com.duynhat.ecommerce_backend.modules.auth;

import com.duynhat.ecommerce_backend.modules.auth.dto.internal.RefreshTokenCreationResult;
import com.duynhat.ecommerce_backend.modules.auth.dto.internal.RefreshTokenRotationResult;
import com.duynhat.ecommerce_backend.modules.user.entity.User;

import java.util.Set;
import java.util.UUID;

public interface RefreshTokenService {

    RefreshTokenCreationResult createRefreshToken(User user);
    UUID revokeRefreshToken(String rawToken);
    RefreshTokenRotationResult rotateRefreshToken(String rawToken);
    long deleteExpiredRefreshTokens();
    Set<UUID> revokeAllRefreshTokens(UUID userId);
    void revokeSession(UUID sessionId);
}
