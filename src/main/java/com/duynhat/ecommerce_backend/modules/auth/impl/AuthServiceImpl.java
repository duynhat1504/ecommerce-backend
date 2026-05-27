package com.duynhat.ecommerce_backend.modules.auth.impl;

import com.duynhat.ecommerce_backend.modules.auth.AuthService;
import com.duynhat.ecommerce_backend.modules.auth.dto.request.RegisterRequest;
import com.duynhat.ecommerce_backend.modules.auth.dto.response.RegisterResponse;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import com.duynhat.ecommerce_backend.modules.user.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public RegisterResponse register(RegisterRequest req) {
        String normalizedEmail = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String passwordHash = passwordEncoder.encode(req.getPassword());
        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordHash)
                .fullName(req.getFullName())
                .role(Role.USER)
                .build();

        try {
            User saved = userRepository.save(user);

            return RegisterResponse.builder()
                    .id(saved.getId())
                    .email(saved.getEmail())
                    .name(saved.getFullName())
                    .build();
        } catch (DataIntegrityViolationException e) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }
}
