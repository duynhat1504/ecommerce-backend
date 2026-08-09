package com.duynhat.ecommerce_backend.modules.auth.jwt;

import java.util.Date;
import java.util.UUID;

public interface JwtService {

    String generateAccessToken(String email, UUID sessionId);
    String extractEmail(String token);
    boolean isTokenValid(String token, String email);
    String extractJti(String token);
    Date extractExpiration(String token);
    UUID extractSessionId(String token);
}
