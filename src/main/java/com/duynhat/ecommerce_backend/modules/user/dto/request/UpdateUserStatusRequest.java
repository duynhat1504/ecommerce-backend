package com.duynhat.ecommerce_backend.modules.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserStatusRequest {

    @NotNull(message = "Active status is required")
    private Boolean active;
}
