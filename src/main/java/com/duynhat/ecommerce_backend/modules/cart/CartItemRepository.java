package com.duynhat.ecommerce_backend.modules.cart;

import com.duynhat.ecommerce_backend.modules.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    Optional<CartItem> findByCartIdAndProductId(UUID cartId, UUID productId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM CartItem ci
            WHERE ci.cart.id = :cartId
              AND ci.product.id = :productId
            """)
    int deleteByCartIdAndProductId(
            @Param("cartId") UUID cartId,
            @Param("productId") UUID productId
    );

    void deleteAllByCartId(UUID cartId);
}
