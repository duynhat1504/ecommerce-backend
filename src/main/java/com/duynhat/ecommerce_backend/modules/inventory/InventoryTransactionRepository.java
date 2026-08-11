package com.duynhat.ecommerce_backend.modules.inventory;

import com.duynhat.ecommerce_backend.modules.inventory.entity.InventoryTransaction;
import com.duynhat.ecommerce_backend.modules.inventory.enums.InventoryTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {

    Page<InventoryTransaction> findByProduct_Id(UUID productId, Pageable pageable);
    Page<InventoryTransaction> findByProduct_IdAndType(
            UUID productId,
            InventoryTransactionType type,
            Pageable pageable
    );
}
