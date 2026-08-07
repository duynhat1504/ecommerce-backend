package com.duynhat.ecommerce_backend.modules.auth;

import com.duynhat.ecommerce_backend.modules.auth.dto.internal.RefreshTokenRotationResult;
import com.duynhat.ecommerce_backend.modules.auth.entity.RefreshToken;
import com.duynhat.ecommerce_backend.modules.user.entity.User;

public interface RefreshTokenService {

    String createRefreshToken(User user);
    RefreshToken validateRefreshToken(String rawToken);
    void revokeRefreshToken(String rawToken);
    RefreshTokenRotationResult rotateRefreshToken(String rawToken);
    long deleteExpiredRefreshTokens();
}
