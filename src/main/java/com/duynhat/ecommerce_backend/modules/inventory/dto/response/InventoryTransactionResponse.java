package com.duynhat.ecommerce_backend.modules.inventory.dto.response;

import com.duynhat.ecommerce_backend.modules.inventory.enums.InventoryTransactionType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class InventoryTransactionResponse {

    private UUID id;
    private UUID productId;
    private String productName;
    private UUID orderId;
    private String orderCode;
    private InventoryTransactionType type;
    private Integer quantityChange;
    private Integer stockBefore;
    private Integer stockAfter;
    private String reason;
    private LocalDateTime createdAt;
}
