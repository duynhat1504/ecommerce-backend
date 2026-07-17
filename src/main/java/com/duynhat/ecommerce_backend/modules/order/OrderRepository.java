package com.duynhat.ecommerce_backend.modules.order;

import com.duynhat.ecommerce_backend.modules.order.dto.request.CreateOrderRequest;
import com.duynhat.ecommerce_backend.modules.order.dto.request.UpdateOrderStatusRequest;
import com.duynhat.ecommerce_backend.modules.order.dto.response.OrderResponse;
import com.duynhat.ecommerce_backend.modules.order.dto.response.OrderSummaryResponse;
import com.duynhat.ecommerce_backend.modules.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByUserId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "items",
            "items.product"
    })
    Optional<Order> findDetailById(UUID id);

    @EntityGraph(attributePaths = {
            "items",
            "items.product"
    })
    Optional<Order> findDetailByIdAndUserId(UUID id, UUID userId);
}
