package com.duynhat.ecommerce_backend.modules.auth.impl;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.modules.auth.AccessTokenBlacklistService;
import com.duynhat.ecommerce_backend.modules.auth.RefreshTokenCompromiseService;
import com.duynhat.ecommerce_backend.modules.auth.RefreshTokenRepository;
import com.duynhat.ecommerce_backend.modules.auth.RefreshTokenService;
import com.duynhat.ecommerce_backend.modules.auth.dto.internal.RefreshTokenCreationResult;
import com.duynhat.ecommerce_backend.modules.auth.dto.internal.RefreshTokenRotationResult;
import com.duynhat.ecommerce_backend.modules.auth.entity.RefreshToken;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Ref;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final int REFRESH_TOKEN_BYTES = 64;

    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RefreshTokenCompromiseService refreshTokenCompromiseService;

    @Autowired
    private AccessTokenBlacklistService accessTokenBlacklistService;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Override
    public RefreshTokenCreationResult createRefreshToken(User user) {
        if (user == null) {
            throw new IllegalStateException("User must not be null");
        }

        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);

        UUID sessionId = UUID.randomUUID();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .sessionId(sessionId)
                .tokenHash(tokenHash)
                .expiresAt(
                        LocalDateTime.now()
                                .plus(Duration.ofMillis(refreshTokenExpiration))
                )
                .build();

        refreshTokenRepository.save(refreshToken);
        return new RefreshTokenCreationResult(rawToken, sessionId);
    }

    @Override
    public UUID revokeRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException("Refresh token is missing");
        }

        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() ->
                        new BadRequestException("Invalid refresh token")
                );

        if (!refreshToken.isRevoked()) {
            refreshToken.revoke();
            refreshTokenRepository.save(refreshToken);
        }

        return refreshToken.getSessionId();
    }

    @Override
    public RefreshTokenRotationResult rotateRefreshToken(String rawToken) {
        validateRawToken(rawToken);

        String currentTokenHash = hashToken(rawToken);

        RefreshToken currentToken = refreshTokenRepository
                .findByTokenHashForUpdate(currentTokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (currentToken.isRevoked()) {
            boolean isRotatedTokenReuse = currentToken.getReplacedByTokenHash() != null;

            refreshTokenCompromiseService.compromiseSession(currentToken.getSessionId());

            accessTokenBlacklistService.blacklistSession(currentToken.getSessionId());

            throw new BadRequestException("Invalid refresh token");
        }

        if (currentToken.isExpired()) {
            throw new BadRequestException("Refresh token has expired");
        }

        User user = currentToken.getUser();

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BadRequestException("User account is inactive");
        }

        String newRawToken = generateRawToken();
        String newTokenHash = hashToken(newRawToken);

        RefreshToken replacementToken = RefreshToken.builder()
                .user(user)
                .sessionId(currentToken.getSessionId())
                .tokenHash(newTokenHash)
                .expiresAt(
                        LocalDateTime.now().plus(
                                Duration.ofMillis(
                                        refreshTokenExpiration
                                )
                        )
                )
                .build();

        refreshTokenRepository.save(replacementToken);

        currentToken.revoke(newTokenHash);
        refreshTokenRepository.save(currentToken);

        return new RefreshTokenRotationResult(user, newRawToken, currentToken.getSessionId());
    }

    @Override
    public long deleteExpiredRefreshTokens() {
        return refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    @Override
    public Set<UUID> revokeAllRefreshTokens(UUID userId) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUser_IdAndRevokedAtIsNull(userId);

        Set<UUID> sessionIds = activeTokens.stream()
                .map(RefreshToken::getSessionId)
                .collect(Collectors.toSet());

        activeTokens.forEach(RefreshToken::revoke);
        refreshTokenRepository.saveAll(activeTokens);

        return sessionIds;
    }

    @Override
    public void revokeSession(UUID sessionId) {
        List<RefreshToken> activeTokens = refreshTokenRepository
                .findAllBySessionIdAndRevokedAtIsNull(sessionId);

        activeTokens.forEach(RefreshToken::revoke);

        refreshTokenRepository.saveAll(activeTokens);
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            byte[] tokenHash = messageDigest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(tokenHash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    exception
            );
        }
    }

    private void validateRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException(
                    "Refresh token is required"
            );
        }
    }
}
