package com.duynhat.ecommerce_backend.integration.dashboard;

import com.duynhat.ecommerce_backend.integration.AbstractIntegrationTest;
import com.duynhat.ecommerce_backend.modules.category.CategoryRepository;
import com.duynhat.ecommerce_backend.modules.category.entity.Category;
import com.duynhat.ecommerce_backend.modules.order.OrderRepository;
import com.duynhat.ecommerce_backend.modules.order.entity.Order;
import com.duynhat.ecommerce_backend.modules.order.enums.OrderStatus;
import com.duynhat.ecommerce_backend.modules.dashboard.impl.AdminDashboardServiceImpl;
import com.duynhat.ecommerce_backend.modules.product.ProductRepository;
import com.duynhat.ecommerce_backend.modules.product.entity.Product;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import com.duynhat.ecommerce_backend.modules.user.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class AdminDashboardIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getSummary_shouldApplyDashboardBusinessRules() throws Exception {
        DashboardBaseline baseline = DashboardBaseline.capture(
                productRepository,
                userRepository,
                orderRepository
        );

        Category category = categoryRepository.saveAndFlush(
                Category.builder()
                        .name("Dashboard Category " + System.nanoTime())
                        .active(true)
                        .build()
        );

        createProduct(category, "Zulu Low", 1, true, false);
        createProduct(category, "Alpha Low", 1, true, false);
        createProduct(category, "Bravo Low", 2, true, false);
        createProduct(category, "Charlie Low", 3, true, false);
        createProduct(category, "Delta Low", 4, true, false);
        createProduct(category, "Echo Low", 5, true, false);
        createProduct(category, "Inactive Product", 2, false, false);
        createProduct(category, "Deleted Product", 1, true, true);

        User activeCustomer = createUser("dashboard-active-" + System.nanoTime(), Role.USER, true);
        createUser("dashboard-inactive-" + System.nanoTime(), Role.USER, false);
        createUser("dashboard-admin-" + System.nanoTime(), Role.ADMIN, true);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        createOrder(activeCustomer, "ORD-DASH-1-" + System.nanoTime(), OrderStatus.PENDING,
                new BigDecimal("10.00"), startOfDay.plusHours(16));
        createOrder(activeCustomer, "ORD-DASH-2-" + System.nanoTime(), OrderStatus.CONFIRMED,
                new BigDecimal("20.00"), startOfDay.plusHours(17));
        createOrder(activeCustomer, "ORD-DASH-3-" + System.nanoTime(), OrderStatus.SHIPPING,
                new BigDecimal("30.00"), startOfDay.plusHours(18));
        createOrder(activeCustomer, "ORD-DASH-4-" + System.nanoTime(), OrderStatus.COMPLETED,
                new BigDecimal("100.00"), startOfDay.plusHours(19));
        createOrder(activeCustomer, "ORD-DASH-5-" + System.nanoTime(), OrderStatus.CANCELLED,
                new BigDecimal("40.00"), startOfDay.plusHours(20));
        createOrder(activeCustomer, "ORD-DASH-6-" + System.nanoTime(), OrderStatus.EXPIRED,
                new BigDecimal("50.00"), startOfDay.plusHours(21));
        createOrder(activeCustomer, "ORD-DASH-OLD-" + System.nanoTime(), OrderStatus.COMPLETED,
                new BigDecimal("50.00"), startOfDay.minusDays(1));

        mockMvc.perform(get("/api/admin/dashboard/summary").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalProducts").value(baseline.totalProducts + 7))
                .andExpect(jsonPath("$.data.activeProducts").value(baseline.activeProducts + 6))
                .andExpect(jsonPath("$.data.lowStockProducts").value(baseline.lowStockProducts + 6))
                .andExpect(jsonPath("$.data.totalUsers").value(baseline.totalUsers + 2))
                .andExpect(jsonPath("$.data.activeUsers").value(baseline.activeUsers + 1))
                .andExpect(jsonPath("$.data.totalOrders").value(baseline.totalOrders + 7))
                .andExpect(jsonPath("$.data.pendingOrders").value(baseline.pendingOrders + 1))
                .andExpect(jsonPath("$.data.confirmedOrders").value(baseline.confirmedOrders + 1))
                .andExpect(jsonPath("$.data.shippingOrders").value(baseline.shippingOrders + 1))
                .andExpect(jsonPath("$.data.completedOrders").value(baseline.completedOrders + 2))
                .andExpect(jsonPath("$.data.cancelledOrders").value(baseline.cancelledOrders + 1))
                .andExpect(jsonPath("$.data.expiredOrders").value(baseline.expiredOrders + 1))
                .andExpect(jsonPath("$.data.todayOrders").value(baseline.todayOrders + 6))
                .andExpect(jsonPath("$.data.todayRevenue")
                        .value(baseline.todayRevenue.add(new BigDecimal("100.00")).doubleValue()))
                .andExpect(jsonPath("$.data.recentOrders.length()").value(5))
                .andExpect(jsonPath("$.data.recentOrders[0].orderCode")
                        .value(org.hamcrest.Matchers.startsWith("ORD-DASH-6-")))
                .andExpect(jsonPath("$.data.lowStockItems.length()").value(5))
                .andExpect(jsonPath("$.data.lowStockItems[0].name").value("Alpha Low"))
                .andExpect(jsonPath("$.data.lowStockItems[1].name").value("Zulu Low"))
                .andExpect(jsonPath("$.data.lowStockItems[4].name").value("Delta Low"));
    }

    private void createProduct(
            Category category,
            String name,
            int stock,
            boolean active,
            boolean deleted
    ) {
        Product product = Product.builder()
                .name(name)
                .description("Dashboard fixture")
                .price(BigDecimal.TEN)
                .stock(stock)
                .active(active)
                .category(category)
                .build();

        if (deleted) {
            product.setDeletedAt(LocalDateTime.now());
        }

        productRepository.saveAndFlush(product);
    }

    private User createUser(String emailPrefix, Role role, boolean active) {
        return userRepository.saveAndFlush(
                User.builder()
                        .email(emailPrefix + "@example.com")
                        .password("password")
                        .fullName("Dashboard User")
                        .role(role)
                        .active(active)
                        .emailVerified(true)
                        .build()
        );
    }

    private void createOrder(
            User user,
            String orderCode,
            OrderStatus status,
            BigDecimal totalAmount,
            LocalDateTime createdAt
    ) {
        Order order = new Order();
        order.setOrderCode(orderCode);
        order.setUser(user);
        order.setStatus(status);
        order.setTotalAmount(totalAmount);
        order.setRecipientName("Dashboard Recipient");
        order.setPhoneNumber("0123456789");
        order.setShippingAddress("Dashboard address");
        orderRepository.saveAndFlush(order);
        order.setCreatedAt(createdAt);
        orderRepository.saveAndFlush(order);
    }

    private record DashboardBaseline(
            long totalProducts,
            long activeProducts,
            long lowStockProducts,
            long totalUsers,
            long activeUsers,
            long totalOrders,
            long pendingOrders,
            long confirmedOrders,
            long shippingOrders,
            long completedOrders,
            long cancelledOrders,
            long expiredOrders,
            long todayOrders,
            BigDecimal todayRevenue
    ) {
        private static DashboardBaseline capture(
                ProductRepository productRepository,
                UserRepository userRepository,
                OrderRepository orderRepository
        ) {
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime nextDayStart = startOfDay.plusDays(1);
            BigDecimal revenue = orderRepository.sumTotalAmountByStatusAndCreatedAtBetween(
                    OrderStatus.COMPLETED,
                    startOfDay,
                    nextDayStart
            );

            return new DashboardBaseline(
                    productRepository.countByDeletedAtIsNull(),
                    productRepository.countByActiveTrueAndDeletedAtIsNull(),
                    productRepository.countByActiveTrueAndDeletedAtIsNullAndStockLessThanEqual(
                            AdminDashboardServiceImpl.LOW_STOCK_THRESHOLD),
                    userRepository.countByRole(Role.USER),
                    userRepository.countByRoleAndActiveTrue(Role.USER),
                    orderRepository.count(),
                    orderRepository.countByStatus(OrderStatus.PENDING),
                    orderRepository.countByStatus(OrderStatus.CONFIRMED),
                    orderRepository.countByStatus(OrderStatus.SHIPPING),
                    orderRepository.countByStatus(OrderStatus.COMPLETED),
                    orderRepository.countByStatus(OrderStatus.CANCELLED),
                    orderRepository.countByStatus(OrderStatus.EXPIRED),
                    orderRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                            startOfDay,
                            nextDayStart
                    ),
                    revenue == null ? BigDecimal.ZERO : revenue
            );
        }
    }
}
