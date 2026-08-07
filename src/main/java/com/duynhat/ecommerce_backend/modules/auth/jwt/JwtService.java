package com.duynhat.ecommerce_backend.modules.auth.jwt;

public interface JwtService {

    String generateAccessToken(String email);
    String extractEmail(String token);
    boolean isTokenValid(String token, String email);
    String extractJti(String token);
}
