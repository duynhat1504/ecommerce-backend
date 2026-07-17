package com.duynhat.ecommerce_backend.modules.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderRequest {

    @NotBlank(message = "Recipient name is required")
    @Size(max = 150, message = "Recipient name must not exceed 150 characters")
    private String recipientName;

    @NotBlank(message = "Phone number is required")
    @Size(max = 30, message = "Phone number must not exceed 30 characters")
    private String phoneNumber;

    @NotBlank(message = "Shpping address is required")
    @Size(max = 500, message = "Shipping address must not exceed 500 characters")
    private String shippingAddress;
}
