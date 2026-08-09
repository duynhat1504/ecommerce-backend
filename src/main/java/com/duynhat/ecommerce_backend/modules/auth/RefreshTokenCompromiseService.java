package com.duynhat.ecommerce_backend.modules.auth;

import java.util.UUID;

public interface RefreshTokenCompromiseService {
    void compromiseSession(UUID sessionId);
}
