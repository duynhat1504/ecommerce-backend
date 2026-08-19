package com.duynhat.ecommerce_backend.modules.auth.impl;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.modules.auth.EmailVerificationService;
import com.duynhat.ecommerce_backend.modules.auth.EmailVerificationTokenRepository;
import com.duynhat.ecommerce_backend.modules.auth.dto.internal.EmailVerificationTokenCreationResult;
import com.duynhat.ecommerce_backend.modules.auth.entity.EmailVerificationToken;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;

    private final UserRepository userRepository;

    @Value("${app.email-verification.expiration-minutes:30}")
    private long expirationMinutes;

    public EmailVerificationServiceImpl(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public EmailVerificationTokenCreationResult createVerificationToken(User user) {
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email is already verified");
        }

        return issueToken(user);
    }

    @Override
    @Transactional
    public void verifyEmail(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidToken();
        }

        String tokenHash = hashToken(rawToken);

        EmailVerificationToken candidate = tokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(this::invalidToken);

        User user = userRepository
                .findByIdForUpdate(candidate.getUser().getId())
                .orElseThrow(this::invalidToken);

        EmailVerificationToken token = tokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(this::invalidToken);

        if (!token.getUser().getId().equals(user.getId())) {
            throw invalidToken();
        }

        if (token.isExpired()) {
            throw invalidToken();
        }

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            tokenRepository.delete(token);

            return;
        }

        user.setEmailVerified(true);

        tokenRepository.delete(token);
    }

    @Override
    @Transactional
    public Optional<EmailVerificationTokenCreationResult> resendVerification(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        Optional<User> userOptional = userRepository
                .findByEmailIgnoreCaseForUpdate(normalizedEmail);

        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        User user = userOptional.get();

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return Optional.empty();
        }

        return Optional.of(issueToken(user));
    }
    
    private EmailVerificationTokenCreationResult
    issueToken(User user) {

        String rawToken =
                generateRawToken();

        String tokenHash =
                hashToken(rawToken);

        LocalDateTime expiresAt =
                LocalDateTime.now()
                        .plusMinutes(
                                expirationMinutes
                        );

        /*
         * Một User chỉ có một row token.
         *
         * Resend không tạo token row thứ 2,
         * mà replace hash + expiration.
         */
        EmailVerificationToken token =
                tokenRepository
                        .findByUser_Id(
                                user.getId()
                        )
                        .orElseGet(
                                () ->
                                        EmailVerificationToken
                                                .builder()
                                                .user(user)
                                                .build()
                        );

        token.setTokenHash(tokenHash);
        token.setExpiresAt(expiresAt);

        tokenRepository.save(token);

        return new EmailVerificationTokenCreationResult(
                user.getEmail(),
                rawToken,
                expiresAt
        );
    }

    private String generateRawToken() {

        byte[] bytes =
                new byte[TOKEN_BYTES];

        secureRandom.nextBytes(bytes);

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hashToken(
            String rawToken
    ) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            rawToken.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat.of()
                    .formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {

            throw new IllegalStateException(
                    "SHA-256 is not available",
                    ex
            );
        }
    }

    private BadRequestException invalidToken() {
        return new BadRequestException(
                "Invalid or expired verification token"
        );
    }
}
