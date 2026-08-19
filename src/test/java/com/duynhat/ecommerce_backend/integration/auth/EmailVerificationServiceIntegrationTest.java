package com.duynhat.ecommerce_backend.integration.auth;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.integration.AbstractIntegrationTest;
import com.duynhat.ecommerce_backend.modules.auth.EmailVerificationService;
import com.duynhat.ecommerce_backend.modules.auth.EmailVerificationTokenRepository;
import com.duynhat.ecommerce_backend.modules.auth.dto.internal.EmailVerificationTokenCreationResult;
import com.duynhat.ecommerce_backend.modules.auth.entity.EmailVerificationToken;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import com.duynhat.ecommerce_backend.modules.user.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestPropertySource(properties = {"app.email-verification.expiration-minutes=30"})
class EmailVerificationServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void createVerificationToken_shouldStoreHashInsteadOfRawToken() throws Exception {
        User user = createUnverifiedUser("token@example.com");

        EmailVerificationTokenCreationResult result =
                emailVerificationService.createVerificationToken(user);

        EmailVerificationToken token = tokenRepository
                .findByUser_Id(user.getId())
                .orElseThrow();

        assertThat(result.rawToken()).isNotBlank();

        assertThat(token.getTokenHash()).isNotEqualTo(result.rawToken());

        assertThat(token.getTokenHash()).isEqualTo(sha256(result.rawToken()));

        assertThat(token.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void verifyEmail_withValidToken_shouldVerifyUserAndDeleteToken() {
        User user = createUnverifiedUser("verify@example.com");

        EmailVerificationTokenCreationResult result =
                emailVerificationService.createVerificationToken(user);

        assertThat(userRepository
                .findById(user.getId())
                .orElseThrow()
                .getEmailVerified()
        ).isFalse();

        emailVerificationService.verifyEmail(result.rawToken());

        User verifiedUser = userRepository.findById(user.getId()).orElseThrow();

        assertThat(verifiedUser.getEmailVerified()).isTrue();

        assertThat(tokenRepository.findByUser_Id(user.getId())).isEmpty();
    }

    @Test
    void verifyEmail_withInvalidToken_shouldRejectAndKeepUserUnverified() {
        User user = createUnverifiedUser("invalid-token@example.com");

        assertThatThrownBy(
                () ->
                        emailVerificationService
                                .verifyEmail("this-token-does-not-exist")
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid or expired verification token");

        User unchanged = userRepository
                .findById(user.getId())
                .orElseThrow();

        assertThat(unchanged.getEmailVerified()).isFalse();
    }

    @Test
    void verifyEmail_withExpiredToken_shouldRejectAndKeepUserUnverified() {
        User user = createUnverifiedUser("expired@example.com");

        EmailVerificationTokenCreationResult result =
                emailVerificationService.createVerificationToken(user);

        jdbcTemplate.update(
                """
                UPDATE email_verification_tokens
                SET expires_at = ?
                WHERE user_id = ?
                """,
                LocalDateTime.now().minusMinutes(1),
                user.getId()
        );

        assertThatThrownBy(
                () ->
                        emailVerificationService
                                .verifyEmail(result.rawToken())
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid or expired verification token");

        User unchanged = userRepository
                .findById(user.getId())
                .orElseThrow();

        assertThat(unchanged.getEmailVerified()).isFalse();
    }

    @Test
    void resendVerification_shouldInvalidateOldTokenAndCreateNewToken() {
        User user = createUnverifiedUser("resend@example.com");

        EmailVerificationTokenCreationResult first =
                emailVerificationService.createVerificationToken(user);

        EmailVerificationTokenCreationResult second = emailVerificationService
                .resendVerification(user.getEmail())
                .orElseThrow();

        assertThat(second.rawToken()).isNotEqualTo(first.rawToken());

        assertThat(tokenRepository.count()).isEqualTo(1);

        assertThatThrownBy(
                () ->
                        emailVerificationService
                                .verifyEmail(first.rawToken())
        )
                .isInstanceOf(BadRequestException.class);

        emailVerificationService.verifyEmail(second.rawToken());

        User verified = userRepository
                .findById(user.getId())
                .orElseThrow();

        assertThat(verified.getEmailVerified()).isTrue();
    }

    @Test
    void resendVerification_whenEmailDoesNotExist_shouldReturnEmpty() {
        assertThat(
                emailVerificationService.resendVerification("not-exist@example.com")
        ).isEmpty();
    }

    @Test
    void resendVerification_whenAlreadyVerified_shouldReturnEmpty() {
        User user = createVerifiedUser("already-verified@example.com");

        assertThat(
                emailVerificationService.resendVerification(user.getEmail())
        ).isEmpty();

        assertThat(tokenRepository.count()).isZero();
    }

    private User createUnverifiedUser(String email) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .password(passwordEncoder.encode("secret123"))
                .fullName("Verification User")
                .role(Role.USER)
                .active(true)
                .emailVerified(false)
                .build()
        );
    }

    private User createVerifiedUser(String email) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .password(passwordEncoder.encode("secret123"))
                .fullName("Verified User")
                .role(Role.USER)
                .active(true)
                .emailVerified(true)
                .build()
        );
    }

    private String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] hash = digest.digest(
                value.getBytes(StandardCharsets.UTF_8)
        );

        return HexFormat.of().formatHex(hash);
    }

    private void cleanDatabase() {
        jdbcTemplate.execute(
                """
                TRUNCATE TABLE
                    email_verification_tokens,
                    users
                CASCADE
                """
        );
    }
}