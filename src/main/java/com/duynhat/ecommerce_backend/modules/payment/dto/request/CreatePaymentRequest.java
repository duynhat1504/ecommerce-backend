package com.duynhat.ecommerce_backend.modules.payment.dto.request;

import com.duynhat.ecommerce_backend.modules.payment.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreatePaymentRequest {

    @NotNull(message = "Order id is required")
    private UUID orderId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod method;

    @NotNull(message = "Payment result is required")
    private Boolean success;
}
