package com.duynhat.ecommerce_backend.modules.auth.cookie;

import com.duynhat.ecommerce_backend.config.RefreshTokenCookieProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshTokenCookieFactory {

    public static final String COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/api/auth";

    private final RefreshTokenCookieProperties cookieProperties;
    private final long refreshTokenExpiration;

    public RefreshTokenCookieFactory(
            RefreshTokenCookieProperties cookieProperties,
            @Value("${jwt.refresh-token-expiration}")
            long refreshTokenExpiration
    ) {
        this.cookieProperties = cookieProperties;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public ResponseCookie create(String refreshToken) {
        return ResponseCookie
                .from(COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(COOKIE_PATH)
                .maxAge(Duration.ofMillis(refreshTokenExpiration))
                .build();
    }

    public ResponseCookie delete() {
        return ResponseCookie
                .from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }
}
