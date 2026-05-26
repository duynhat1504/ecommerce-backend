package com.duynhat.ecommerce_backend.modules.auth.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class RegisterResponse {

    private String id;
    private String email;
    private String name;

}
