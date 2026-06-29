package com.duynhat.ecommerce_backend.modules.user.dto.response;

import com.duynhat.ecommerce_backend.modules.user.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class UserResponse {

    private UUID id;
    private String fullName;
    private String email;
    private Role role;
    private Boolean active;
}
