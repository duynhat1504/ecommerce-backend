package com.duynhat.ecommerce_backend.unit.auth;

import com.duynhat.ecommerce_backend.modules.auth.jwt.impl.JwtServiceImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceImplTest {

    private static final String SECRET =
            "unit-test-secret-key-unit-test-secret-key-123456";

    private JwtServiceImpl jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtServiceImpl();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 3_600_000L);
    }

    @Test
    void generateAccessToken_shouldContainEmailJtiAndBeValid() {
        UUID sessionId = UUID.randomUUID();
        String token = jwtService.generateAccessToken("user@example.com", sessionId);

        assertThat(jwtService.extractEmail(token)).isEqualTo("user@example.com");
        assertThat(jwtService.extractJti(token)).isNotBlank();
        assertThat(jwtService.isTokenValid(token, "user@example.com")).isTrue();
    }

    @Test
    void generateAccessToken_shouldGenerateUniqueJti() {
        UUID sessionId = UUID.randomUUID();

        String token1 = jwtService.generateAccessToken("user@example.com", sessionId);
        String token2 = jwtService.generateAccessToken("user@example.com", sessionId);

        assertThat(jwtService.extractJti(token1)).isNotEqualTo(jwtService.extractJti(token2));
        assertThat(jwtService.extractSessionId(token1)).isEqualTo(sessionId);
        assertThat(jwtService.extractSessionId(token2)).isEqualTo(sessionId);
    }

    @Test
    void isTokenValid_withDifferentEmail_shouldReturnFalse() {
        UUID sessionId = UUID.randomUUID();
        String token = jwtService.generateAccessToken("user@example.com", sessionId);

        assertThat(jwtService.isTokenValid(token, "other@example.com")).isFalse();
    }

    @Test
    void expiredToken_shouldBeRejected() {
        UUID sessionId = UUID.randomUUID();
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", -1_000L);
        String token = jwtService.generateAccessToken("user@example.com", sessionId);

        assertThatThrownBy(() -> jwtService.isTokenValid(token, "user@example.com"))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tamperedToken_shouldBeRejected() {
        UUID sessionId = UUID.randomUUID();
        String token = jwtService.generateAccessToken("user@example.com", sessionId);

        assertThatThrownBy(() -> jwtService.extractEmail(token + "tampered"))
                .isInstanceOf(JwtException.class);
    }
}
