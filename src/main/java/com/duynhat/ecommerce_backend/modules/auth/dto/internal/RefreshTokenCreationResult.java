package com.duynhat.ecommerce_backend.modules.auth.dto.internal;

import java.util.UUID;

public record RefreshTokenCreationResult(String refreshToken, UUID sessionId) {
}
