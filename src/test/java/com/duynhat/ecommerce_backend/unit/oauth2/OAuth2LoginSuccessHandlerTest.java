package com.duynhat.ecommerce_backend.unit.oauth2;

import com.duynhat.ecommerce_backend.config.RefreshTokenCookieProperties;
import com.duynhat.ecommerce_backend.modules.auth.RefreshTokenService;
import com.duynhat.ecommerce_backend.modules.auth.cookie.RefreshTokenCookieFactory;
import com.duynhat.ecommerce_backend.modules.auth.dto.internal.RefreshTokenCreationResult;
import com.duynhat.ecommerce_backend.modules.auth.jwt.JwtService;
import com.duynhat.ecommerce_backend.modules.user.UserService;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import com.duynhat.ecommerce_backend.security.oauth2.OAuth2LoginSuccessHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RefreshTokenCookieFactory refreshTokenCookieFactory;

    private OAuth2LoginSuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        successHandler = new OAuth2LoginSuccessHandler(
                        userService,
                        refreshTokenService,
                        refreshTokenCookieFactory
                );

        ReflectionTestUtils.setField(
                successHandler,
                "frontendUrl",
                "http://localhost:5173"
        );

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
    }

    @Test
    void onAuthenticationSuccess_withValidGoogleUser_shouldCreateTokenAndRedirect() throws Exception {
        User user = new User();
        user.setEmail("google@example.com");

        when(oAuth2User.getAttribute("sub")).thenReturn("google-123");
        when(oAuth2User.getAttribute("email")).thenReturn("google@example.com");
        when(oAuth2User.getAttribute("name")).thenReturn("Google User");
        when(userService.findOrCreateGoogleUser(
                "google-123",
                "google@example.com",
                "Google User"
        )).thenReturn(user);

        RefreshTokenCreationResult refreshTokenResult = new RefreshTokenCreationResult(
                "refresh-token",
                UUID.randomUUID()
        );

        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshTokenResult);

        ResponseCookie refreshTokenCookie = ResponseCookie
                .from(
                        "refresh_token",
                        "refresh-token"
                )
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/auth")
                .build();

        when(refreshTokenCookieFactory.create("refresh-token")).thenReturn(refreshTokenCookie);

        MockHttpServletResponse response = new MockHttpServletResponse();
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshTokenResult);
        successHandler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                authentication
        );

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/oauth2/callback");
        assertThat(response.getHeader("Set-Cookie"))
                .contains("refresh_token=refresh-token")
                .contains("HttpOnly");
        verify(userService).findOrCreateGoogleUser(
                "google-123",
                "google@example.com",
                "Google User"
        );
        verify(refreshTokenService).createRefreshToken(user);
    }

    @Test
    void onAuthenticationSuccess_withoutEmail_shouldRedirectToLoginError()
            throws Exception {
        when(oAuth2User.getAttribute("sub")).thenReturn("google-123");
        when(oAuth2User.getAttribute("email")).thenReturn(null);

        MockHttpServletResponse response = new MockHttpServletResponse();
        successHandler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                authentication
        );

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/login?error=google_login_failed");
        verifyNoInteractions(userService, jwtService, refreshTokenService);
    }

    @Test
    void onAuthenticationSuccess_withoutGoogleId_shouldRedirectToLoginError()
            throws Exception {
        when(oAuth2User.getAttribute("sub")).thenReturn(null);
        when(oAuth2User.getAttribute("email")).thenReturn("google@example.com");

        MockHttpServletResponse response = new MockHttpServletResponse();
        successHandler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                authentication
        );

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/login?error=google_login_failed");
        verifyNoInteractions(userService, jwtService, refreshTokenService);
    }
}
