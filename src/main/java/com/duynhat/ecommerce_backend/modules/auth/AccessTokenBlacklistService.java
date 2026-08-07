package com.duynhat.ecommerce_backend.modules.auth;

public interface AccessTokenBlacklistService {
    
    void blacklist(String accessToken);
    boolean isBlacklisted(String jti);
}
