package com.duynhat.ecommerce_backend.modules.auth.dto.internal;

import com.duynhat.ecommerce_backend.modules.auth.dto.response.RefreshResponse;

public record RefreshResult(
        RefreshResponse refreshResponse,
        String refreshToken
) {
}
