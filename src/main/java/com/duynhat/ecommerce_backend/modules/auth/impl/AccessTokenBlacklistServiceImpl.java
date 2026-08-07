package com.duynhat.ecommerce_backend.modules.auth.impl;

import com.duynhat.ecommerce_backend.modules.auth.AccessTokenBlacklistService;
import com.duynhat.ecommerce_backend.modules.auth.jwt.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class AccessTokenBlacklistServiceImpl implements AccessTokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "auth:blacklist:access-token:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JwtService jwtService;

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
                BLACKLIST_PREFIX + jti,
                "revoked",
                ttl
        );
    }

    @Override
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(BLACKLIST_PREFIX + jti)
        );
    }
}
