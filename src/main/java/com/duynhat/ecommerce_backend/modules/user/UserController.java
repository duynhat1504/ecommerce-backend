package com.duynhat.ecommerce_backend.modules.user;

import com.duynhat.ecommerce_backend.common.core.dto.ApiResponse;
import com.duynhat.ecommerce_backend.modules.user.dto.response.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        UserResponse user = userService.getCurrentUser(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get current user successfully",
                        user
                )
        );
    }

}
