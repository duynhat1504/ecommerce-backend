package com.duynhat.ecommerce_backend.modules.order.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateOrderRequest {

    @NotNull(message = "Shipping address is required")
    private UUID addressId;
}
