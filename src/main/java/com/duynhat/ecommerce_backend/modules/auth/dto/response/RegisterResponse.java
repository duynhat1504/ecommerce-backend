package com.duynhat.ecommerce_backend.modules.auth.dto.response;

import com.duynhat.ecommerce_backend.modules.user.enums.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@Builder
public class RegisterResponse {

    private UUID id;
    private String email;
    private String fullName;
    private Role role;
}
