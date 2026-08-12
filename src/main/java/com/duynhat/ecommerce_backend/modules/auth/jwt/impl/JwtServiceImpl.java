package com.duynhat.ecommerce_backend.modules.auth.jwt.impl;

import com.duynhat.ecommerce_backend.modules.auth.jwt.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Override
    public String generateAccessToken(
            String email,
            UUID sessionId,
            LocalDateTime sessionExpiresAt
    ) {

        Instant now = Instant.now();

        Instant normalAccessExpiry = now.plusMillis(accessTokenExpiration);

        Instant sessionExpiry = sessionExpiresAt
                .atZone(ZoneId.systemDefault())
                .toInstant();

        Instant effectiveExpiry = normalAccessExpiry
                .isBefore(sessionExpiry)
                ? normalAccessExpiry
                : sessionExpiry;

        if (!effectiveExpiry.isAfter(now)) {
            throw new IllegalStateException("Session has already expired");
        }

        Date issuedAt = Date.from(now);

        Date expiryDate = Date.from(effectiveExpiry);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(email)
                .claim("sid", sessionId.toString())
                .issuedAt(issuedAt)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    @Override
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    @Override
    public boolean isTokenValid(String token, String email) {
        String tokenEmail = extractEmail(token);
        return tokenEmail.equals(email) && !isTokenExpired(token);
    }

    @Override
    public String extractJti(String token) {
        return extractAllClaims(token).getId();
    }

    @Override
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    @Override
    public UUID extractSessionId(String token) {
        String sessionId = extractAllClaims(token).get("sid", String.class);

        return UUID.fromString(sessionId);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractAllClaims(token).getExpiration();
        return expiration.before(new Date());
    }
}
