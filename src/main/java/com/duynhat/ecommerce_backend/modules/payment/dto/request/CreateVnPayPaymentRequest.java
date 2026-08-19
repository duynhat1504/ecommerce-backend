package com.duynhat.ecommerce_backend.modules.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateVnPayPaymentRequest {

    @NotNull(message = "Order id is required")
    private UUID orderId;
}
