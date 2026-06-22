package com.duynhat.ecommerce_backend.modules.auth.mapper;

import com.duynhat.ecommerce_backend.modules.auth.dto.response.AuthResponse;
import com.duynhat.ecommerce_backend.modules.user.entity.User;

public class AuthMapper {

    private AuthMapper() {
    };

    public static AuthResponse toAuthResponse(User user) {
        return AuthResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}
