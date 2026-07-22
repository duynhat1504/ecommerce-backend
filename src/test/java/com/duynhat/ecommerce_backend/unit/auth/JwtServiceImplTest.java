package com.duynhat.ecommerce_backend.unit.auth;

import com.duynhat.ecommerce_backend.modules.auth.jwt.impl.JwtServiceImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
    void generateAccessToken_shouldContainEmailAndBeValid() {
        String token = jwtService.generateAccessToken("user@example.com");

        assertThat(jwtService.extractEmail(token)).isEqualTo("user@example.com");
        assertThat(jwtService.isTokenValid(token, "user@example.com")).isTrue();
    }

    @Test
    void isTokenValid_withDifferentEmail_shouldReturnFalse() {
        String token = jwtService.generateAccessToken("user@example.com");

        assertThat(jwtService.isTokenValid(token, "other@example.com")).isFalse();
    }

    @Test
    void expiredToken_shouldBeRejected() {
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", -1_000L);
        String token = jwtService.generateAccessToken("user@example.com");

        assertThatThrownBy(() -> jwtService.isTokenValid(token, "user@example.com"))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tamperedToken_shouldBeRejected() {
        String token = jwtService.generateAccessToken("user@example.com");

        assertThatThrownBy(() -> jwtService.extractEmail(token + "tampered"))
                .isInstanceOf(JwtException.class);
    }
}
