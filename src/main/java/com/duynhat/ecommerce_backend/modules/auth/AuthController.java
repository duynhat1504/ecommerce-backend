package com.duynhat.ecommerce_backend.modules.auth;

import com.duynhat.ecommerce_backend.common.core.dto.ApiResponse;
import com.duynhat.ecommerce_backend.config.RefreshTokenCookieProperties;
import com.duynhat.ecommerce_backend.modules.auth.dto.internal.LoginResult;
import com.duynhat.ecommerce_backend.modules.auth.dto.internal.RefreshResult;
import com.duynhat.ecommerce_backend.modules.auth.dto.request.LoginRequest;
import com.duynhat.ecommerce_backend.modules.auth.dto.request.RegisterRequest;
import com.duynhat.ecommerce_backend.modules.auth.dto.response.AuthResponse;
import com.duynhat.ecommerce_backend.modules.auth.dto.response.RefreshResponse;
import com.duynhat.ecommerce_backend.modules.auth.dto.response.RegisterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Autowired
    private AuthService authService;

    @Autowired
    private RefreshTokenCookieProperties cookieProperties;

    @PostMapping("/register")
    @Operation(
            summary = "Register",
            description = "Create a new user account"
    )
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @RequestBody @Valid RegisterRequest req
    ) {
        RegisterResponse register = authService.register(req);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Register successfully",
                                register
                        )
                );
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login",
            description = "Authenticate user and return JWT access token"
    )
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletResponse servletResponse
    ) {
        LoginResult loginResult = authService.login(req);

        ResponseCookie refreshTokenCookie = ResponseCookie
                .from(REFRESH_TOKEN_COOKIE, loginResult.refreshToken())
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path("/api/auth")
                .maxAge(Duration.ofMillis(refreshTokenExpiration))
                .build();

        servletResponse.addHeader(
                HttpHeaders.SET_COOKIE,
                refreshTokenCookie.toString()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Login successfully",
                        loginResult.authResponse()
                )
        );
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Create a new access token and rotate the refresh token"
    )
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String rawRefreshToken,
            HttpServletResponse servletResponse

    ) {
        RefreshResult refreshResult = authService.refresh(rawRefreshToken);

        ResponseCookie refreshTokenCookie = ResponseCookie
                .from(
                        REFRESH_TOKEN_COOKIE,
                        refreshResult.refreshToken()
                )
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path("/api/auth")
                .maxAge(Duration.ofMillis(refreshTokenExpiration))
                .build();

        servletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Refresh token successfully",
                        refreshResult.refreshResponse()
                )
        );
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout",
            description = "Revoke the current refresh token and clear its cookie"
    )
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(
                    name = REFRESH_TOKEN_COOKIE,
                    required = false
            ) String rawRefreshToken,
            @RequestHeader(
                    name = HttpHeaders.AUTHORIZATION,
                    required = false
            ) String authorizationHeader,
            HttpServletResponse servletResponse
    ) {
        String accessToken = extractBearerToken(authorizationHeader);

        authService.logout(rawRefreshToken, accessToken);

        ResponseCookie deletedRefreshTokenCookie =
                ResponseCookie
                        .from(REFRESH_TOKEN_COOKIE, "")
                        .httpOnly(true)
                        .secure(cookieProperties.secure())
                        .sameSite(cookieProperties.sameSite())
                        .path("/api/auth")
                        .maxAge(Duration.ZERO)
                        .build();

        servletResponse.addHeader(HttpHeaders.SET_COOKIE, deletedRefreshTokenCookie.toString());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Logout successfully",
                        null
                )
        );
    }

    @PostMapping("/logout-all")
    @Operation(
            summary = "Logout all devices",
            description = "Revoke all refresh tokens of the current user"
    )
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            Authentication authentication,
            HttpServletResponse servletResponse
    ) {

        authService.logoutAll(
                authentication.getName()
        );

        ResponseCookie deletedRefreshTokenCookie = ResponseCookie
                .from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .build();

        servletResponse.addHeader(
                HttpHeaders.SET_COOKIE,
                deletedRefreshTokenCookie.toString()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Logged out from all devices successfully",
                        null
                )
        );
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        return authorizationHeader.substring(7);
    }
}
