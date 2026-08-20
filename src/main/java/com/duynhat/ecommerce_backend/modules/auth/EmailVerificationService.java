package com.duynhat.ecommerce_backend.modules.auth;

import com.duynhat.ecommerce_backend.modules.auth.dto.internal.EmailVerificationTokenCreationResult;
import com.duynhat.ecommerce_backend.modules.user.entity.User;

import java.util.Optional;

public interface EmailVerificationService {

    EmailVerificationTokenCreationResult createVerificationToken(User user);
    void verifyEmail(String rawToken);
    Optional<EmailVerificationTokenCreationResult> resendVerification(String email);
}
