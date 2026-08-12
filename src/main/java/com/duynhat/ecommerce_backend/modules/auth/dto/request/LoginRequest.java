package com.duynhat.ecommerce_backend.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email is invalid")
    @Size(max = 100, message = "Email must be mos")
    private String email;

    @NotBlank(message = "Password must not be blank")
    @Size(max = 100, message = "")
    private String password;

}
