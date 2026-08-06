package com.duynhat.ecommerce_backend.modules.auth.dto.internal;

import com.duynhat.ecommerce_backend.modules.user.entity.User;

public record RefreshTokenRotationResult(User user, String refreshToken) {
}
