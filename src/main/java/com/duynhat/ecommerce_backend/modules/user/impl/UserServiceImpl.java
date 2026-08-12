package com.duynhat.ecommerce_backend.modules.user.impl;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.common.core.exception.ResourceNotFoundException;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.UserService;
import com.duynhat.ecommerce_backend.modules.user.dto.response.UserResponse;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import com.duynhat.ecommerce_backend.modules.user.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserResponse getCurrentUser(Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return toResponse(user);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElse(null);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public User findOrCreateGoogleUser(
            String googleId,
            String email,
            String fullName
    ) {

        if (googleId == null || googleId.isBlank()) {
            throw new BadRequestException("Google user id is missing");
        }

        if (email == null || email.isBlank()) {
            throw new BadRequestException("Google email is missing");
        }

        String normalizedGoogleId = googleId.trim();

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (normalizedEmail.length() > 100) {
            throw new BadRequestException("Google email exceeds supported length");
        }

        if (normalizedGoogleId.length() > 255) {
            throw new BadRequestException("Google user id exceeds supported length");
        }

        String normalizedFullName = normalizeGoogleFullName(fullName, normalizedEmail);

        User user = userRepository
                .findByGoogleId(normalizedGoogleId)
                .or(() -> userRepository.findByEmail(normalizedEmail))
                .map(existingUser -> {
                    existingUser.setGoogleId(normalizedGoogleId);
                    if (existingUser.getFullName() == null || existingUser.getFullName().isBlank()) {
                        existingUser.setFullName(normalizedFullName);
                    }
                    return existingUser;
                })
                .orElseGet(() -> User.builder()
                        .email(normalizedEmail)
                        .fullName(normalizedFullName)
                        .googleId(normalizedGoogleId)
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .role(Role.USER)
                        .active(true)
                        .build());

        return userRepository.save(user);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.getActive())
                .build();
    }

    private String normalizeGoogleFullName(String fullName, String email) {
        String normalized = fullName == null ? "" : fullName.trim();

        if (normalized.isBlank()) {
            int atIndex = email.indexOf('@');

            normalized = atIndex > 0 ? email.substring(0, atIndex) : "Google User";
        }

        if (normalized.length() > 100) {
            normalized = normalized.substring(0, 100);
        }

        return normalized;
    }
}
