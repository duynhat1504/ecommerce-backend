package com.duynhat.ecommerce_backend.modules.user;

import com.duynhat.ecommerce_backend.common.core.dto.ApiResponse;
import com.duynhat.ecommerce_backend.modules.user.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User", description = "User APIs")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get current user",
            description = "Get information of the authenticated user"
    )
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
