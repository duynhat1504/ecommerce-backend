package com.duynhat.ecommerce_backend.modules.user;

import com.duynhat.ecommerce_backend.modules.user.dto.response.UserResponse;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import org.springframework.security.core.Authentication;

public interface UserService {

    UserResponse getCurrentUser(Authentication authentication);
    User findByEmail(String email);
    boolean existsByEmail(String email);
    User findOrCreateGoogleUser(String googleId, String email, String fullName);

}
