package com.duynhat.ecommerce_backend.modules.order;

import com.duynhat.ecommerce_backend.modules.order.dto.request.CreateOrderRequest;
import com.duynhat.ecommerce_backend.modules.order.dto.request.UpdateOrderStatusRequest;
import com.duynhat.ecommerce_backend.modules.order.dto.response.OrderResponse;
import com.duynhat.ecommerce_backend.modules.order.dto.response.OrderSummaryResponse;
import com.duynhat.ecommerce_backend.modules.order.enums.OrderStatus;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);
    Page<OrderSummaryResponse> getMyOrders(int page, int size);
    OrderResponse getMyOrderById(UUID orderId);
    Page<OrderSummaryResponse> getAllOrders(
            OrderStatus status,
            String orderCode,
            int page,
            int size
    );
    OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request);
    OrderResponse cancelMyOrder(UUID orderId);
    OrderResponse getOrderById(UUID orderId);
}
