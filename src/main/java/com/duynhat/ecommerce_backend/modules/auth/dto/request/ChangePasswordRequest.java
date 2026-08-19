package com.duynhat.ecommerce_backend.modules.auth.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
    private String confirmNewPassword;
}
