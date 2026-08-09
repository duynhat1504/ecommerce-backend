package com.duynhat.ecommerce_backend.modules.auth.impl;

import com.duynhat.ecommerce_backend.modules.auth.AccessTokenBlacklistService;
import com.duynhat.ecommerce_backend.modules.auth.jwt.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class AccessTokenBlacklistServiceImpl implements AccessTokenBlacklistService {

    private static final String ACCESS_TOKEN_BLACKLIST_PREFIX = "auth:blacklist:access-token:";
    private static final String SESSION_BLACKLIST_PREFIX = "auth:blacklist:session:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JwtService jwtService;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    public AccessTokenBlacklistServiceImpl(StringRedisTemplate redisTemplate, JwtService jwtService) {
        this.redisTemplate = redisTemplate;
        this.jwtService = jwtService;
    }

    @Override
    public void blacklist(String accessToken) {
        String jti = jwtService.extractJti(accessToken);
        Date expiration = jwtService.extractExpiration(accessToken);

        Duration ttl = Duration.between(
                Instant.now(),
                expiration.toInstant()
        );

        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }

        redisTemplate.opsForValue().set(
                ACCESS_TOKEN_BLACKLIST_PREFIX + jti,
                "revoked",
                ttl
        );
    }

    @Override
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(ACCESS_TOKEN_BLACKLIST_PREFIX + jti)
        );
    }

    @Override
    public void blacklistSession(UUID sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID must not be null");
        }

        Duration ttl =Duration.ofMillis(accessTokenExpiration);

        redisTemplate.opsForValue().set(
                SESSION_BLACKLIST_PREFIX + sessionId,
                "revoked",
                ttl
        );
    }

    @Override
    public boolean isSessionBlacklisted(UUID sessionId) {
        if (sessionId == null) {
            return false;
        }

        return Boolean.TRUE.equals(
                redisTemplate.hasKey(SESSION_BLACKLIST_PREFIX + sessionId)
        );
    }
}
