package com.duynhat.ecommerce_backend.modules.cart;

import com.duynhat.ecommerce_backend.modules.cart.entity.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    @EntityGraph(attributePaths = {
            "items",
            "items.product"
    })
    Optional<Cart> findByUserId(UUID userId);
}
