package com.duynhat.ecommerce_backend.security.oauth2;

import com.duynhat.ecommerce_backend.config.RefreshTokenCookieProperties;
import com.duynhat.ecommerce_backend.modules.auth.RefreshTokenService;
import com.duynhat.ecommerce_backend.modules.auth.cookie.RefreshTokenCookieFactory;
import com.duynhat.ecommerce_backend.modules.auth.dto.internal.RefreshTokenCreationResult;
import com.duynhat.ecommerce_backend.modules.auth.jwt.JwtService;
import com.duynhat.ecommerce_backend.modules.user.UserService;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserService userService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenCookieFactory refreshTokenCookieFactory;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public OAuth2LoginSuccessHandler(
            UserService userService,
            RefreshTokenService refreshTokenService,
            RefreshTokenCookieFactory refreshTokenCookieFactory
    ) {
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenCookieFactory = refreshTokenCookieFactory;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String googleId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String fullName = oAuth2User.getAttribute("name");

        if (email == null
                || email.isBlank()
                || googleId == null
                || googleId.isBlank()
        ) {
            response.sendRedirect(
                    buildFrontendRedirect(
                            "/login",
                            "error",
                            "google_login_failed"
                    )
            );

            return;
        }

        User savedUser = userService.findOrCreateGoogleUser(googleId, email, fullName);

        RefreshTokenCreationResult refreshTokenResult = refreshTokenService.createRefreshToken(savedUser);
        String refreshToken = refreshTokenResult.refreshToken();

        ResponseCookie refreshTokenCookie = refreshTokenCookieFactory.create(refreshToken);

        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        response.sendRedirect(frontendUrl + "/oauth2/callback");
    }

    private String buildFrontendRedirect(String path, String queryParam, String value) {
        return UriComponentsBuilder.fromUriString(frontendUrl)
                .path(path)
                .queryParam(queryParam, value)
                .build()
                .toUriString();
    }
}
