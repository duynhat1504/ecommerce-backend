package com.duynhat.ecommerce_backend.modules.order;

import com.duynhat.ecommerce_backend.modules.order.dto.request.CreateOrderRequest;
import com.duynhat.ecommerce_backend.modules.order.dto.request.UpdateOrderStatusRequest;
import com.duynhat.ecommerce_backend.modules.order.dto.response.OrderResponse;
import com.duynhat.ecommerce_backend.modules.order.dto.response.OrderSummaryResponse;
import com.duynhat.ecommerce_backend.modules.order.entity.Order;
import com.duynhat.ecommerce_backend.modules.order.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByUserId(UUID userId, Pageable pageable);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    Page<Order> findByOrderCodeContainingIgnoreCase(
            String orderCode,
            Pageable pageable
    );

    Page<Order> findByStatusAndOrderCodeContainingIgnoreCase(
            OrderStatus status,
            String orderCode,
            Pageable pageable
    );

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o
            FROM Order o
            WHERE o.id = :orderId
            """)
    Optional<Order> findByIdForUpdate(
            @Param("orderId") UUID orderId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT o
        FROM Order o
        WHERE o.id = :orderId
          AND o.user.id = :userId
        """)
    Optional<Order> findByIdAndUserIdForUpdate(
            @Param("orderId") UUID orderId,
            @Param("userId") UUID userId
    );
}
