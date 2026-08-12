package com.duynhat.ecommerce_backend.modules.auth.dto.internal;

import com.duynhat.ecommerce_backend.modules.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record RefreshTokenRotationResult(
        User user,
        String refreshToken,
        UUID sessionId,
        LocalDateTime expiresAt) {
}
