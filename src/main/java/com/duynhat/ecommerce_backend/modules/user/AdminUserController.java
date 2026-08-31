package com.duynhat.ecommerce_backend.modules.user;

import com.duynhat.ecommerce_backend.common.core.dto.ApiResponse;
import com.duynhat.ecommerce_backend.common.core.dto.PageResponse;
import com.duynhat.ecommerce_backend.modules.user.dto.request.AdminUserQueryRequest;
import com.duynhat.ecommerce_backend.modules.user.dto.request.UpdateUserRoleRequest;
import com.duynhat.ecommerce_backend.modules.user.dto.request.UpdateUserStatusRequest;
import com.duynhat.ecommerce_backend.modules.user.dto.response.AdminUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin User", description = "Admin user management APIs")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "Get users for admin")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> findUsers(
            @ModelAttribute AdminUserQueryRequest req
    ) {
        Page<AdminUserResponse> users = adminUserService.findUsers(req);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get users successfully",
                        PageResponse.from(users)
                )
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user detail for admin")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getById(
            @PathVariable UUID id
    ) {
        AdminUserResponse user = adminUserService.getById(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get user successfully",
                        user
                )
        );
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update user active status")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest req
    ) {
        AdminUserResponse user = adminUserService.updateStatus(id, req);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Update user status successfully",
                        user
                )
        );
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "Update user role")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleRequest req
    ) {
        AdminUserResponse user = adminUserService.updateRole(id, req);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Update user role successfully",
                        user
                )
        );
    }
}
