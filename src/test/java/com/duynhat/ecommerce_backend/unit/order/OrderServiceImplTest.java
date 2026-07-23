package com.duynhat.ecommerce_backend.unit.order;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.common.core.exception.ResourceNotFoundException;
import com.duynhat.ecommerce_backend.modules.cart.CartItemRepository;
import com.duynhat.ecommerce_backend.modules.cart.CartRepository;
import com.duynhat.ecommerce_backend.modules.order.OrderRepository;
import com.duynhat.ecommerce_backend.modules.order.dto.request.UpdateOrderStatusRequest;
import com.duynhat.ecommerce_backend.modules.order.entity.Order;
import com.duynhat.ecommerce_backend.modules.order.enums.OrderStatus;
import com.duynhat.ecommerce_backend.modules.order.impl.OrderServiceImpl;
import com.duynhat.ecommerce_backend.modules.product.ProductRepository;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CartItemRepository cartItemRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @ParameterizedTest
    @CsvSource({
            "PENDING, CONFIRMED",
            "CONFIRMED, SHIPPING",
            "SHIPPING, COMPLETED"
    })
    void updateOrderStatus_withValidTransition_shouldSave(
            OrderStatus currentStatus,
            OrderStatus newStatus
    ) {
        UUID orderId = UUID.randomUUID();
        Order order = order(orderId, currentStatus);
        UpdateOrderStatusRequest request = request(newStatus);
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        var response = orderService.updateOrderStatus(orderId, request);

        assertThat(response.getStatus()).isEqualTo(newStatus);
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrderStatus_withSameStatus_shouldBeIdempotent() {
        UUID orderId = UUID.randomUUID();
        Order order = order(orderId, OrderStatus.CONFIRMED);
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        var response = orderService.updateOrderStatus(orderId, request(OrderStatus.CONFIRMED));

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(productRepository);
    }

    @Test
    void updateOrderStatus_withInvalidTransition_shouldRejectWithoutSaving() {
        UUID orderId = UUID.randomUUID();
        Order order = order(orderId, OrderStatus.PENDING);
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, request(OrderStatus.COMPLETED)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot change order status from PENDING to COMPLETED");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_whenOrderMissing_shouldThrowNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, request(OrderStatus.CONFIRMED)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found");
    }

    @ParameterizedTest
    @CsvSource({"-1, 10", "0, 0", "0, 101"})
    void getAllOrders_withInvalidPagination_shouldReject(int page, int size) {
        assertThatThrownBy(() -> orderService.getAllOrders(page, size))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(orderRepository);
    }

    private UpdateOrderStatusRequest request(OrderStatus status) {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(status);
        return request;
    }

    private Order order(UUID id, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setOrderCode("ORD-UNIT-TEST");
        order.setStatus(status);
        order.setTotalAmount(BigDecimal.ZERO);
        return order;
    }
}
