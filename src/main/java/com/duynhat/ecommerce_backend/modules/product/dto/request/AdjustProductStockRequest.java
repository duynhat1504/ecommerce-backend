package com.duynhat.ecommerce_backend.modules.product.dto.request;


import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdjustProductStockRequest {

    @NotNull(message = "Stock adjustment is required")
    private Integer quantity;
}
