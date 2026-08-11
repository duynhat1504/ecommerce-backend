package com.duynhat.ecommerce_backend.modules.inventory;

import com.duynhat.ecommerce_backend.modules.inventory.entity.InventoryTransaction;
import com.duynhat.ecommerce_backend.modules.inventory.enums.InventoryTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {

    Page<InventoryTransaction> findByProduct_Id(UUID productId, Pageable pageable);
    Page<InventoryTransaction> findByProduct_IdAndType(
            UUID productId,
            InventoryTransactionType type,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "product",
            "order",
            "performedBy"
    })
    @Query("""
        SELECT t
        FROM InventoryTransaction t
        WHERE t.product.id = :productId
          AND (:type IS NULL OR t.type = :type)
          AND (:fromDate IS NULL OR t.createdAt >= :fromDate)
          AND (:toDateExclusive IS NULL OR t.createdAt < :toDateExclusive)
        """)
    Page<InventoryTransaction> findByProductWithFilters(
            @Param("productId") UUID productId,
            @Param("type") InventoryTransactionType type,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDateExclusive") LocalDateTime toDateExclusive,
            Pageable pageable
    );
}
