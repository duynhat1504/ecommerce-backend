package com.duynhat.ecommerce_backend.modules.product.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdjustProductStockRequest {

    @NotNull(message = "Stock adjustment is required")
    private Integer quantity;

    @NotBlank(message = "Reason is required")
    @Size(max = 255, message = "Reason must not exceed 255 characters")
    private String reason;
}
