package com.duynhat.ecommerce_backend.unit.auth;

import com.duynhat.ecommerce_backend.modules.auth.RefreshTokenService;
import com.duynhat.ecommerce_backend.modules.auth.dto.internal.LoginResult;
import com.duynhat.ecommerce_backend.modules.auth.dto.internal.RefreshTokenCreationResult;
import com.duynhat.ecommerce_backend.modules.auth.dto.request.LoginRequest;
import com.duynhat.ecommerce_backend.modules.auth.dto.request.RegisterRequest;
import com.duynhat.ecommerce_backend.modules.auth.dto.response.AuthResponse;
import com.duynhat.ecommerce_backend.modules.auth.dto.response.RegisterResponse;
import com.duynhat.ecommerce_backend.modules.auth.impl.AuthServiceImpl;
import com.duynhat.ecommerce_backend.modules.auth.jwt.JwtService;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import com.duynhat.ecommerce_backend.modules.user.enums.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_withValidRequest_shouldNormalizeEmailAndEncodePassword() {
        RegisterRequest request = registerRequest();
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        RegisterResponse response = authService.register(request);

        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getRole()).isEqualTo(Role.USER);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(userCaptor.getValue().getActive()).isTrue();
    }

    @Test
    void register_withDuplicateEmail_shouldThrowConflictException() {
        RegisterRequest request = registerRequest();
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("Email already exists");

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void login_withValidUser_shouldReturnToken() {
        LoginRequest request = loginRequest("secret123");
        User user = activeUser();
        RefreshTokenCreationResult refreshTokenResult = new RefreshTokenCreationResult("refresh-token", UUID.randomUUID());
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "encoded-password")).thenReturn(true);
        when(jwtService.generateAccessToken("user@example.com")).thenReturn("jwt-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshTokenResult);

        LoginResult result = authService.login(request);
        AuthResponse response = result.authResponse();

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");

        verify(refreshTokenService).createRefreshToken(user);
    }

    @Test
    void login_whenUserDoesNotExist_shouldThrowBadCredentials() {
        LoginRequest request = loginRequest("secret123");
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void login_withWrongPassword_shouldThrowBadCredentials() {
        LoginRequest request = loginRequest("wrong-password");
        User user = activeUser();
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password"))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");

        verifyNoInteractions(jwtService);
    }

    @Test
    void login_whenUserIsInactive_shouldThrowBadCredentials() {
        LoginRequest request = loginRequest("secret123");
        User user = activeUser();
        user.setActive(false);
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "encoded-password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");

        verifyNoInteractions(jwtService);
    }

    private RegisterRequest registerRequest() {
        return RegisterRequest.builder()
                .fullName("Test User")
                .email(" USER@example.com ")
                .password("secret123")
                .build();
    }

    private LoginRequest loginRequest(String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(" USER@example.com ");
        request.setPassword(password);
        return request;
    }

    private User activeUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .fullName("Test User")
                .password("encoded-password")
                .role(Role.USER)
                .active(true)
                .build();
    }
}
