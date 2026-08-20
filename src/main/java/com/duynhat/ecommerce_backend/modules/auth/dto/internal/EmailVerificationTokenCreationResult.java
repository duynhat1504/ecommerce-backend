package com.duynhat.ecommerce_backend.modules.auth.dto.internal;

import java.time.LocalDateTime;

public record EmailVerificationTokenCreationResult(
        String email,
        String rawToken,
        LocalDateTime expiresAt
) {
}
