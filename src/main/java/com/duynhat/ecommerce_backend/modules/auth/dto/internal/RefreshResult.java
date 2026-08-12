package com.duynhat.ecommerce_backend.modules.auth.dto.internal;

import com.duynhat.ecommerce_backend.modules.auth.dto.response.RefreshResponse;

import java.time.LocalDateTime;

public record RefreshResult(
        RefreshResponse refreshResponse,
        String refreshToken,
        LocalDateTime refreshTokenExpiresAt
) {
}
