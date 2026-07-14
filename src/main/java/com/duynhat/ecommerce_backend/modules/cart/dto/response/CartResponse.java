package com.duynhat.ecommerce_backend.modules.cart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private UUID cartId;
    private List<CartItemResponse> items;
    private Integer totalItems;
    private BigDecimal totalPrice;
}
