package com.duynhat.ecommerce_backend.modules.cart;

import com.duynhat.ecommerce_backend.modules.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    Optional<CartItem> findByCartIdAndProductId(UUID cartId, UUID productId);
    void deleteAllByCartId(UUID cartId);
}
