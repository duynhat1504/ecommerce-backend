package com.duynhat.ecommerce_backend.modules.inventory;

import com.duynhat.ecommerce_backend.modules.inventory.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {
}
