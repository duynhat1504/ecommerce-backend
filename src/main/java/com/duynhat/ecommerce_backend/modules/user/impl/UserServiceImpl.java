package com.duynhat.ecommerce_backend.modules.user.impl;

import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.UserService;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import com.duynhat.ecommerce_backend.modules.user.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    public User findOrCreateGoogleUser(String googleId, String email, String fullName) {
        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByGoogleId(googleId)
                .or(() -> userRepository.findByEmail(normalizedEmail))
                .map(existingUser -> {
                    existingUser.setGoogleId(googleId);
                    if (existingUser.getFullName() == null || existingUser.getFullName().isBlank()) {
                        existingUser.setFullName(fullName);
                    }
                    return existingUser;
                })
                .orElseGet(() -> User.builder()
                        .email(normalizedEmail)
                        .fullName(fullName)
                        .googleId(googleId)
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .role(Role.USER)
                        .active(true)
                        .build());

        return userRepository.save(user);
    }
}
