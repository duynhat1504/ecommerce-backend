package com.duynhat.ecommerce_backend.modules.auth;

import com.duynhat.ecommerce_backend.modules.auth.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
    Optional<EmailVerificationToken> findByUser_Id(UUID userId);
    void deleteByUser_Id(UUID userId);
}