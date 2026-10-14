package com.duynhat.ecommerce_backend.unit.dashboard;

import com.duynhat.ecommerce_backend.modules.dashboard.dto.response.AdminDashboardSummaryResponse;
import com.duynhat.ecommerce_backend.modules.dashboard.impl.AdminDashboardServiceImpl;
import com.duynhat.ecommerce_backend.modules.order.OrderRepository;
import com.duynhat.ecommerce_backend.modules.order.entity.Order;
import com.duynhat.ecommerce_backend.modules.order.enums.OrderStatus;
import com.duynhat.ecommerce_backend.modules.product.ProductRepository;
import com.duynhat.ecommerce_backend.modules.product.entity.Product;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.enums.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;

    @InjectMocks
    private AdminDashboardServiceImpl adminDashboardService;

    @Test
    void getSummary_shouldAggregateRepositoryResultsAndMapDashboardLists() {
        Order newestOrder = order(
                "ORD-NEWEST",
                OrderStatus.COMPLETED,
                new BigDecimal("350000"),
                LocalDateTime.now().minusMinutes(1)
        );
        Product lowStockProduct = product("V60 Dripper", 3, true);

        when(productRepository.countByDeletedAtIsNull()).thenReturn(120L);
        when(productRepository.countByActiveTrueAndDeletedAtIsNull()).thenReturn(105L);
        when(productRepository.countByActiveTrueAndDeletedAtIsNullAndStockLessThanEqual(
                AdminDashboardServiceImpl.LOW_STOCK_THRESHOLD)).thenReturn(8L);
        when(userRepository.countByRole(Role.USER)).thenReturn(350L);
        when(userRepository.countByRoleAndActiveTrue(Role.USER)).thenReturn(330L);
        when(orderRepository.count()).thenReturn(460L);
        when(orderRepository.countByStatus(OrderStatus.PENDING)).thenReturn(12L);
        when(orderRepository.countByStatus(OrderStatus.CONFIRMED)).thenReturn(5L);
        when(orderRepository.countByStatus(OrderStatus.SHIPPING)).thenReturn(7L);
        when(orderRepository.countByStatus(OrderStatus.COMPLETED)).thenReturn(420L);
        when(orderRepository.countByStatus(OrderStatus.CANCELLED)).thenReturn(10L);
        when(orderRepository.countByStatus(OrderStatus.EXPIRED)).thenReturn(6L);
        when(orderRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(9L);
        when(orderRepository.sumTotalAmountByStatusAndCreatedAtBetween(
                org.mockito.ArgumentMatchers.eq(OrderStatus.COMPLETED),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(new BigDecimal("2450000"));
        when(orderRepository.findAllByOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.any())).thenReturn(List.of(newestOrder));
        when(productRepository
                .findByActiveTrueAndDeletedAtIsNullAndStockLessThanEqualOrderByStockAscNameAsc(
                        org.mockito.ArgumentMatchers.eq(
                                AdminDashboardServiceImpl.LOW_STOCK_THRESHOLD),
                        org.mockito.ArgumentMatchers.any())).thenReturn(List.of(lowStockProduct));

        AdminDashboardSummaryResponse response = adminDashboardService.getSummary();

        assertThat(response.getTotalProducts()).isEqualTo(120L);
        assertThat(response.getActiveProducts()).isEqualTo(105L);
        assertThat(response.getLowStockProducts()).isEqualTo(8L);
        assertThat(response.getTotalUsers()).isEqualTo(350L);
        assertThat(response.getActiveUsers()).isEqualTo(330L);
        assertThat(response.getTotalOrders()).isEqualTo(460L);
        assertThat(response.getPendingOrders()).isEqualTo(12L);
        assertThat(response.getConfirmedOrders()).isEqualTo(5L);
        assertThat(response.getShippingOrders()).isEqualTo(7L);
        assertThat(response.getCompletedOrders()).isEqualTo(420L);
        assertThat(response.getCancelledOrders()).isEqualTo(10L);
        assertThat(response.getExpiredOrders()).isEqualTo(6L);
        assertThat(response.getTodayOrders()).isEqualTo(9L);
        assertThat(response.getTodayRevenue()).isEqualByComparingTo("2450000");
        assertThat(response.getRecentOrders()).singleElement().satisfies(recentOrder -> {
            assertThat(recentOrder.getId()).isEqualTo(newestOrder.getId());
            assertThat(recentOrder.getOrderCode()).isEqualTo("ORD-NEWEST");
            assertThat(recentOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            assertThat(recentOrder.getTotalAmount()).isEqualByComparingTo("350000");
        });
        assertThat(response.getLowStockItems()).singleElement().satisfies(item -> {
            assertThat(item.getId()).isEqualTo(lowStockProduct.getId());
            assertThat(item.getName()).isEqualTo("V60 Dripper");
            assertThat(item.getStock()).isEqualTo(3);
            assertThat(item.getActive()).isTrue();
        });

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findAllByOrderByCreatedAtDesc(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderRepository).countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                startCaptor.capture(),
                endCaptor.capture());
        assertThat(startCaptor.getValue().toLocalTime()).isEqualTo(java.time.LocalTime.MIDNIGHT);
        assertThat(endCaptor.getValue()).isEqualTo(startCaptor.getValue().plusDays(1));
    }

    @Test
    void getSummary_whenRevenueQueryReturnsNull_shouldReturnZero() {
        when(orderRepository.sumTotalAmountByStatusAndCreatedAtBetween(
                org.mockito.ArgumentMatchers.eq(OrderStatus.COMPLETED),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(null);
        when(orderRepository.findAllByOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(productRepository
                .findByActiveTrueAndDeletedAtIsNullAndStockLessThanEqualOrderByStockAscNameAsc(
                        org.mockito.ArgumentMatchers.eq(
                                AdminDashboardServiceImpl.LOW_STOCK_THRESHOLD),
                        org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        AdminDashboardSummaryResponse response = adminDashboardService.getSummary();

        assertThat(response.getTodayRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private Order order(
            String orderCode,
            OrderStatus status,
            BigDecimal totalAmount,
            LocalDateTime createdAt
    ) {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setOrderCode(orderCode);
        order.setStatus(status);
        order.setTotalAmount(totalAmount);
        order.setCreatedAt(createdAt);
        return order;
    }

    private Product product(String name, int stock, boolean active) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName(name);
        product.setStock(stock);
        product.setActive(active);
        return product;
    }
}
