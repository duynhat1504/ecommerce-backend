package com.duynhat.ecommerce_backend.modules.auth.impl;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.modules.auth.RefreshTokenRepository;
import com.duynhat.ecommerce_backend.modules.auth.RefreshTokenService;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final int REFRESH_TOKEN_BYTES = 64;

    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Override
    public String createRefreshToken(User user) {
        if (user == null) {
            throw new IllegalStateException("User must not be null");
        }

        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(
                        LocalDateTime.now()
                                .plus(Duration.ofMillis(refreshTokenExpiration))
                )
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String rawToken) {
        validateRawToken(rawToken);

        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new BadRequestException("Refresh token has been revoked");
        }

        if (refreshToken.isExpired()) {
            throw new BadRequestException("Refresh token has expired");
        }

        if (!refreshToken.getUser().getActive()) {
            throw new BadRequestException("User account is inactive");
        }

        return refreshToken;
    }

    @Override
    public void revokeRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        String tokenHash = hashToken(rawToken);

        refreshTokenRepository
                .findByTokenHash(tokenHash)
                .ifPresent(refreshToken -> {
                    if (!refreshToken.isRevoked()) {
                        refreshToken.revoke();
                        refreshTokenRepository.save(refreshToken);
                    }
                });
    }

    @Override
    public RefreshTokenRotationResult rotateRefreshToken(String rawToken) {
        validateRawToken(rawToken);

        String currentTokenHash = hashToken(rawToken);

        RefreshToken currentToken = refreshTokenRepository
                .findByTokenHashForUpdate(currentTokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (currentToken.isRevoked()) {
            throw new BadRequestException("Refresh token has been revoked");
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

        return new RefreshTokenRotationResult(user, newRawToken);
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
