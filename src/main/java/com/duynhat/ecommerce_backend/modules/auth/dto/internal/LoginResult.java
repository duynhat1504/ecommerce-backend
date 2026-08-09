package com.duynhat.ecommerce_backend.modules.auth.dto.internal;

import com.duynhat.ecommerce_backend.modules.auth.dto.response.AuthResponse;

public record LoginResult(AuthResponse authResponse, String refreshToken) {
}
