package com.duynhat.ecommerce_backend.modules.auth;

import java.util.UUID;

public interface AccessTokenBlacklistService {
    
    void blacklist(String accessToken);
    boolean isBlacklisted(String jti);
    void blacklistSession(UUID sessionId);
    boolean isSessionBlacklisted(UUID sessionId);
}
