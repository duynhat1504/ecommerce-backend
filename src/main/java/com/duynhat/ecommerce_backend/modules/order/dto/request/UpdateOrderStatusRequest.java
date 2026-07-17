package com.duynhat.ecommerce_backend.modules.order.dto.request;

import com.duynhat.ecommerce_backend.modules.order.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOrderStatusRequest {

    @NotNull(message = "Order status is required")
    private OrderStatus status;
}
