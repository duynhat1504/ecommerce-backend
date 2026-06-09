package com.duynhat.ecommerce_backend.modules.auth.impl;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.modules.auth.AuthService;
import com.duynhat.ecommerce_backend.modules.auth.dto.request.LoginRequest;
import com.duynhat.ecommerce_backend.modules.auth.dto.request.RegisterRequest;
import com.duynhat.ecommerce_backend.modules.auth.dto.response.AuthResponse;
import com.duynhat.ecommerce_backend.modules.auth.dto.response.RegisterResponse;
import com.duynhat.ecommerce_backend.modules.auth.mapper.AuthMapper;
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
    public AuthResponse register(RegisterRequest req) {
        String normalizedEmail = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("Invalid email or password");
        }

        String passwordHash = passwordEncoder.encode(req.getPassword());
        User user = User.builder()
                .fullName(req.getFullName())
                .email(normalizedEmail)
                .password(passwordHash)
                .fullName(req.getFullName())
                .role(Role.USER)
                .active(true)
                .build();

        try {
            User saved = userRepository.save(user);
            return AuthMapper.toAuthResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Invalid email or password");
        }
    }

    @Override
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        boolean isPasswordMatched = passwordEncoder.matches(
                req.getPassword(),
                user.getPassword()
        );

        if (!isPasswordMatched) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.getActive()) {
            throw new BadRequestException("Account is disabled");
        }

        return AuthMapper.toAuthResponse(user);
    }
}
