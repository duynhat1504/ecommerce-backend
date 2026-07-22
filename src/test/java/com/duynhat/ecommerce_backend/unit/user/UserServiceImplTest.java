package com.duynhat.ecommerce_backend.unit.user;

import com.duynhat.ecommerce_backend.common.core.exception.ResourceNotFoundException;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import com.duynhat.ecommerce_backend.modules.user.enums.Role;
import com.duynhat.ecommerce_backend.modules.user.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void findOrCreateGoogleUser_whenGoogleIdExists_shouldReuseUser() {
        User existingUser = user("existing@example.com", "Existing User");
        existingUser.setGoogleId("google-123");
        when(userRepository.findByGoogleId("google-123"))
                .thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        User result = userService.findOrCreateGoogleUser(
                "google-123",
                "other@example.com",
                "Other Name"
        );

        assertThat(result).isSameAs(existingUser);
        assertThat(result.getEmail()).isEqualTo("existing@example.com");
        verify(userRepository, never()).findByEmail(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void findOrCreateGoogleUser_whenEmailExists_shouldLinkGoogleAccount() {
        User existingUser = user("local@example.com", "");
        when(userRepository.findByGoogleId("google-123"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("local@example.com"))
                .thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        User result = userService.findOrCreateGoogleUser(
                "google-123",
                " LOCAL@example.com ",
                "Google Name"
        );

        assertThat(result.getGoogleId()).isEqualTo("google-123");
        assertThat(result.getFullName()).isEqualTo("Google Name");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void findOrCreateGoogleUser_whenUserDoesNotExist_shouldCreateActiveUser() {
        when(userRepository.findByGoogleId("google-123"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded-random-password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.findOrCreateGoogleUser(
                "google-123",
                " New@example.com ",
                "New Google User"
        );

        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getGoogleId()).isEqualTo("google-123");
        assertThat(result.getFullName()).isEqualTo("New Google User");
        assertThat(result.getPassword()).isEqualTo("encoded-random-password");
        assertThat(result.getRole()).isEqualTo(Role.USER);
        assertThat(result.getActive()).isTrue();

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(passwordCaptor.capture());
        assertThat(passwordCaptor.getValue()).isNotBlank();
    }

    @Test
    void getCurrentUser_whenUserDoesNotExist_shouldThrowNotFound() {
        when(authentication.getName()).thenReturn("missing@example.com");
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser(authentication))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    private User user(String email, String fullName) {
        return User.builder()
                .email(email)
                .fullName(fullName)
                .password("encoded-password")
                .role(Role.USER)
                .active(true)
                .build();
    }
}
