package com.duynhat.ecommerce_backend.integration.order;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.integration.AbstractIntegrationTest;
import com.duynhat.ecommerce_backend.modules.cart.CartItemRepository;
import com.duynhat.ecommerce_backend.modules.cart.CartRepository;
import com.duynhat.ecommerce_backend.modules.cart.CartService;
import com.duynhat.ecommerce_backend.modules.cart.dto.request.AddCartItemRequest;
import com.duynhat.ecommerce_backend.modules.cart.entity.Cart;
import com.duynhat.ecommerce_backend.modules.category.CategoryRepository;
import com.duynhat.ecommerce_backend.modules.category.entity.Category;
import com.duynhat.ecommerce_backend.modules.order.OrderRepository;
import com.duynhat.ecommerce_backend.modules.order.OrderService;
import com.duynhat.ecommerce_backend.modules.order.dto.request.CreateOrderRequest;
import com.duynhat.ecommerce_backend.modules.order.dto.request.UpdateOrderStatusRequest;
import com.duynhat.ecommerce_backend.modules.order.dto.response.OrderResponse;
import com.duynhat.ecommerce_backend.modules.order.entity.Order;
import com.duynhat.ecommerce_backend.modules.order.enums.OrderStatus;
import com.duynhat.ecommerce_backend.modules.product.ProductRepository;
import com.duynhat.ecommerce_backend.modules.product.entity.Product;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import com.duynhat.ecommerce_backend.modules.user.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class OrderIntegrationTest extends AbstractIntegrationTest {

    private static final String USER_EMAIL = "order-integration@example.com";

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    private User user;
    private Category category;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        user = userRepository.saveAndFlush(
                User.builder()
                        .email(USER_EMAIL)
                        .password("test-password")
                        .fullName("Order Integration User")
                        .role(Role.USER)
                        .active(true)
                        .build()
        );

        category = categoryRepository.saveAndFlush(
                Category.builder()
                        .name("Order Test Category")
                        .description("Category for order integration tests")
                        .active(true)
                        .build()
        );

        authenticateAsUser();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        cleanDatabase();
    }

    @Test
    void createOrder_withEmptyCart_shouldRejectAndNotCreateOrder() {
        Cart emptyCart = new Cart();
        emptyCart.setUser(user);
        cartRepository.saveAndFlush(emptyCart);

        assertThatThrownBy(() -> orderService.createOrder(createOrderRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cart is empty");

        assertThat(orderRepository.count()).isZero();
        assertThat(cartRepository.existsById(emptyCart.getId())).isTrue();
    }

    @Test
    void createOrder_withValidCart_shouldDecreaseProductStock() {
        Product product = createProduct("Stock Product", "100.00", 10);
        addToCart(product, 3);

        OrderResponse response = orderService.createOrder(createOrderRequest());

        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("300.00");
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock())
                .isEqualTo(7);
    }

    @Test
    void createOrder_withValidCart_shouldClearCartItems() {
        Product firstProduct = createProduct("First Product", "100.00", 10);
        Product secondProduct = createProduct("Second Product", "50.00", 10);
        addToCart(firstProduct, 1);
        addToCart(secondProduct, 2);
        Cart cart = getUserCart();
        assertThat(countCartItems(cart.getId())).isEqualTo(2L);

        orderService.createOrder(createOrderRequest());

        assertThat(countCartItems(cart.getId())).isZero();
        assertThat(cartItemRepository.findAllByCartId(cart.getId())).isEmpty();
        assertThat(cartRepository.existsById(cart.getId())).isTrue();
    }

    @Test
    void createOrder_shouldStoreUnitPriceSnapshot() {
        Product product = createProduct("Snapshot Product", "125.50", 10);
        addToCart(product, 2);

        OrderResponse response = orderService.createOrder(createOrderRequest());

        product.setPrice(new BigDecimal("999.99"));
        productRepository.saveAndFlush(product);

        Order persistedOrder = orderRepository.findDetailById(response.getId()).orElseThrow();
        assertThat(persistedOrder.getItems())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getProductName()).isEqualTo("Snapshot Product");
                    assertThat(item.getUnitPrice()).isEqualByComparingTo("125.50");
                    assertThat(item.getSubtotal()).isEqualByComparingTo("251.00");
                });
    }

    @Test
    void createOrder_whenOneProductHasInsufficientStock_shouldRollbackEverything() {
        Product availableProduct = createProduct("Available Product", "100.00", 5);
        Product insufficientProduct = createProduct("Insufficient Product", "50.00", 5);
        addToCart(availableProduct, 2);
        addToCart(insufficientProduct, 2);
        Cart cart = getUserCart();

        insufficientProduct.setStock(1);
        productRepository.saveAndFlush(insufficientProduct);

        assertThatThrownBy(() -> orderService.createOrder(createOrderRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Insufficient stock for product: Insufficient Product");

        assertThat(orderRepository.count()).isZero();
        assertThat(countCartItems(cart.getId())).isEqualTo(2L);
        assertThat(productRepository.findById(availableProduct.getId()).orElseThrow().getStock())
                .isEqualTo(5);
        assertThat(productRepository.findById(insufficientProduct.getId()).orElseThrow().getStock())
                .isEqualTo(1);
    }

    @Test
    void cancelPendingOrder_shouldRestoreProductStock() {
        Product product = createProduct("Cancellation Product", "100.00", 10);
        addToCart(product, 3);
        OrderResponse createdOrder = orderService.createOrder(createOrderRequest());
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock())
                .isEqualTo(7);

        OrderResponse cancelledOrder = orderService.updateOrderStatus(
                createdOrder.getId(),
                updateStatusRequest(OrderStatus.CANCELLED)
        );

        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock())
                .isEqualTo(10);
    }

    @Test
    void cancelOrderTwice_shouldNotRestoreStockTwice() {
        Product product = createProduct("Idempotent Cancellation Product", "100.00", 10);
        addToCart(product, 3);
        OrderResponse createdOrder = orderService.createOrder(createOrderRequest());

        orderService.updateOrderStatus(
                createdOrder.getId(),
                updateStatusRequest(OrderStatus.CANCELLED)
        );
        OrderResponse secondCancellation = orderService.updateOrderStatus(
                createdOrder.getId(),
                updateStatusRequest(OrderStatus.CANCELLED)
        );

        assertThat(secondCancellation.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock())
                .isEqualTo(10);
    }

    @Test
    void updateOrderStatus_withInvalidTransition_shouldReturn400() throws Exception {
        Product product = createProduct("Invalid Transition Product", "100.00", 10);
        addToCart(product, 1);
        OrderResponse createdOrder = orderService.createOrder(createOrderRequest());
        authenticateAsAdmin();

        mockMvc.perform(
                        put("/api/admin/orders/{id}/status", createdOrder.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "COMPLETED"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Cannot change order status from PENDING to COMPLETED"));

        assertThat(orderRepository.findById(createdOrder.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.PENDING);
    }

    private Product createProduct(String name, String price, int stock) {
        return productRepository.saveAndFlush(
                Product.builder()
                        .name(name)
                        .description(name + " description")
                        .price(new BigDecimal(price))
                        .stock(stock)
                        .active(true)
                        .category(category)
                        .build()
        );
    }

    private void addToCart(Product product, int quantity) {
        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(product.getId());
        request.setQuantity(quantity);
        cartService.addItem(request);
    }

    private CreateOrderRequest createOrderRequest() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setRecipientName("Order Recipient");
        request.setPhoneNumber("0901234567");
        request.setShippingAddress("123 Test Street");
        return request;
    }

    private UpdateOrderStatusRequest updateStatusRequest(OrderStatus status) {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(status);
        return request;
    }

    private Cart getUserCart() {
        return cartRepository.findByUserId(user.getId()).orElseThrow();
    }

    private long countCartItems(UUID cartId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cart_items WHERE cart_id = ?",
                Long.class,
                cartId
        );
        return count == null ? 0 : count;
    }

    private void authenticateAsUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        USER_EMAIL,
                        null,
                        List.of()
                )
        );
    }

    private void authenticateAsAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin-integration@example.com",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );
    }

    private void cleanDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    order_items,
                    orders,
                    cart_items,
                    carts,
                    products,
                    categories,
                    users
                CASCADE
                """);
    }
}
