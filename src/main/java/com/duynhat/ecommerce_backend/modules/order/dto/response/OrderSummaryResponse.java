package com.duynhat.ecommerce_backend.modules.order.dto.response;

import com.duynhat.ecommerce_backend.modules.order.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryResponse {

    private UUID id;
    private String orderCode;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private Integer totalItems;
    private LocalDateTime createdAt;
}
