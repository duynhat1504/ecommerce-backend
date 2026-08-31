package com.duynhat.ecommerce_backend.modules.user.dto.request;

import com.duynhat.ecommerce_backend.modules.user.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRoleRequest {

    @NotNull(message = "Role is required")
    private Role role;
}
