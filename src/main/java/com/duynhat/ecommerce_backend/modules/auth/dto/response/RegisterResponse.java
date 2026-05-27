package com.duynhat.ecommerce_backend.modules.auth.dto.response;

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
    private String name;

}
