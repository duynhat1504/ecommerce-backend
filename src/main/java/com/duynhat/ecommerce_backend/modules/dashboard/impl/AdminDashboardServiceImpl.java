package com.duynhat.ecommerce_backend.modules.dashboard.impl;

import com.duynhat.ecommerce_backend.modules.dashboard.AdminDashboardService;
import com.duynhat.ecommerce_backend.modules.dashboard.dto.response.AdminDashboardSummaryResponse;
import com.duynhat.ecommerce_backend.modules.dashboard.dto.response.DashboardLowStockProductResponse;
import com.duynhat.ecommerce_backend.modules.dashboard.dto.response.DashboardRecentOrderResponse;
import com.duynhat.ecommerce_backend.modules.order.OrderRepository;
import com.duynhat.ecommerce_backend.modules.order.entity.Order;
import com.duynhat.ecommerce_backend.modules.order.enums.OrderStatus;
import com.duynhat.ecommerce_backend.modules.product.ProductRepository;
import com.duynhat.ecommerce_backend.modules.product.entity.Product;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    public static final int LOW_STOCK_THRESHOLD = 5;
    private static final int DASHBOARD_LIST_LIMIT = 5;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse getSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime nextDayStart = today.plusDays(1).atStartOfDay();

        BigDecimal todayRevenue = orderRepository
                .sumTotalAmountByStatusAndCreatedAtBetween(
                        OrderStatus.COMPLETED,
                        startOfDay,
                        nextDayStart
                );

        return AdminDashboardSummaryResponse.builder()
                .totalProducts(productRepository.countByDeletedAtIsNull())
                .activeProducts(productRepository.countByActiveTrueAndDeletedAtIsNull())
                .lowStockProducts(productRepository
                        .countByActiveTrueAndDeletedAtIsNullAndStockLessThanEqual(
                                LOW_STOCK_THRESHOLD))
                .totalUsers(userRepository.countByRole(Role.USER))
                .activeUsers(userRepository.countByRoleAndActiveTrue(Role.USER))
                .totalOrders(orderRepository.count())
                .pendingOrders(orderRepository.countByStatus(OrderStatus.PENDING))
                .confirmedOrders(orderRepository.countByStatus(OrderStatus.CONFIRMED))
                .shippingOrders(orderRepository.countByStatus(OrderStatus.SHIPPING))
                .completedOrders(orderRepository.countByStatus(OrderStatus.COMPLETED))
                .cancelledOrders(orderRepository.countByStatus(OrderStatus.CANCELLED))
                .expiredOrders(orderRepository.countByStatus(OrderStatus.EXPIRED))
                .todayOrders(orderRepository
                        .countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                startOfDay,
                                nextDayStart))
                .todayRevenue(todayRevenue == null ? BigDecimal.ZERO : todayRevenue)
                .recentOrders(orderRepository
                        .findAllByOrderByCreatedAtDesc(
                                PageRequest.of(0, DASHBOARD_LIST_LIMIT))
                        .stream()
                        .map(this::toRecentOrderResponse)
                        .toList())
                .lowStockItems(productRepository
                        .findByActiveTrueAndDeletedAtIsNullAndStockLessThanEqualOrderByStockAscNameAsc(
                                LOW_STOCK_THRESHOLD,
                                PageRequest.of(0, DASHBOARD_LIST_LIMIT))
                        .stream()
                        .map(this::toLowStockProductResponse)
                        .toList())
                .build();
    }

    private DashboardRecentOrderResponse toRecentOrderResponse(Order order) {
        return DashboardRecentOrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private DashboardLowStockProductResponse toLowStockProductResponse(Product product) {
        return DashboardLowStockProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .stock(product.getStock())
                .active(product.getActive())
                .build();
    }
}
