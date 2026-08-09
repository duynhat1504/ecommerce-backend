package com.duynhat.ecommerce_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.refresh-cookie")
public record RefreshTokenCookieProperties(boolean secure, String sameSite) {
}
