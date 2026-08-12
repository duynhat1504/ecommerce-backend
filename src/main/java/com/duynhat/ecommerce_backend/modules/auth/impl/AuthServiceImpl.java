package com.duynhat.ecommerce_backend.modules.auth.impl;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.modules.auth.AccessTokenBlacklistService;
import com.duynhat.ecommerce_backend.modules.auth.AuthService;
import com.duynhat.ecommerce_backend.modules.auth.RefreshTokenService;
import com.duynhat.ecommerce_backend.modules.auth.dto.internal.LoginResult;
import com.duynhat.ecommerce_backend.modules.auth.dto.internal.RefreshResult;
import com.duynhat.ecommerce_backend.modules.auth.dto.internal.RefreshTokenCreationResult;
import com.duynhat.ecommerce_backend.modules.auth.dto.internal.RefreshTokenRotationResult;
import com.duynhat.ecommerce_backend.modules.auth.dto.request.LoginRequest;
import com.duynhat.ecommerce_backend.modules.auth.dto.request.RegisterRequest;
import com.duynhat.ecommerce_backend.modules.auth.dto.response.AuthResponse;
import com.duynhat.ecommerce_backend.modules.auth.dto.response.RefreshResponse;
import com.duynhat.ecommerce_backend.modules.auth.dto.response.RegisterResponse;
import com.duynhat.ecommerce_backend.modules.auth.jwt.JwtService;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.UserService;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import com.duynhat.ecommerce_backend.modules.user.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserService userService;

    @Autowired
    private AccessTokenBlacklistService accessTokenBlacklistService;

    @Override
    public RegisterResponse register(RegisterRequest req) {
        String normalizedEmail = req.getEmail().trim().toLowerCase();

        String normalizedFullName = req.getFullName().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DataIntegrityViolationException("Email already exists");
        }

        String passwordHash = passwordEncoder.encode(req.getPassword());
        User user = User.builder()
                .fullName(normalizedFullName)
                .email(normalizedEmail)
                .password(passwordHash)
                .role(Role.USER)
                .active(true)
                .build();

        User saved = userRepository.save(user);

        return RegisterResponse.builder()
                .id(saved.getId())
                .email(saved.getEmail())
                .fullName(saved.getFullName())
                .role(saved.getRole())
                .build();
    }

    @Override
    @Transactional
    public LoginResult login(LoginRequest req) {
        String normalizedEmail = req.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        boolean isPasswordMatched = passwordEncoder.matches(
                req.getPassword(),
                user.getPassword()
        );

        if (!isPasswordMatched) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        RefreshTokenCreationResult refreshTokenResult = refreshTokenService.createRefreshToken(user);
        String accessToken = jwtService.generateAccessToken(user.getEmail(), refreshTokenResult.sessionId());

        AuthResponse authResponse =  AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();

        return new LoginResult(authResponse, refreshTokenResult.refreshToken());
    }

    @Override
    @Transactional
    public RefreshResult refresh(String rawRefreshToken) {
        RefreshTokenRotationResult rotationResult = refreshTokenService.rotateRefreshToken(rawRefreshToken);

        String newAccessToken = jwtService.generateAccessToken(
                rotationResult.user().getEmail(),
                rotationResult.sessionId()
        );

        RefreshResponse refreshResponse = new RefreshResponse(newAccessToken, "Bearer");

        return new RefreshResult(refreshResponse, rotationResult.refreshToken());
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken, String accessToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BadRequestException("Refresh token cookie is missing");
        }

        UUID sessionId = refreshTokenService.revokeRefreshToken(rawRefreshToken);
        accessTokenBlacklistService.blacklistSession(sessionId);

        if (accessToken != null && !accessToken.isBlank()) {
            accessTokenBlacklistService.blacklist(accessToken);
        }
    }

    @Override
    @Transactional
    public void logoutAll(String email) {
        User user = userService.findByEmail(email);

        Set<UUID> sessionIds = refreshTokenService.revokeAllRefreshTokens(user.getId());

        sessionIds.forEach(accessTokenBlacklistService::blacklistSession);
    }
}
