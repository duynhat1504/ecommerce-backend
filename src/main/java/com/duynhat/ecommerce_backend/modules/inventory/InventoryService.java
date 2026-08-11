package com.duynhat.ecommerce_backend.modules.inventory;

import com.duynhat.ecommerce_backend.modules.inventory.dto.response.InventoryTransactionResponse;
import com.duynhat.ecommerce_backend.modules.inventory.enums.InventoryTransactionType;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface InventoryService {

    Page<InventoryTransactionResponse> getProductTransactions(
            UUID productId,
            InventoryTransactionType type,
            int page,
            int size
    );
}
