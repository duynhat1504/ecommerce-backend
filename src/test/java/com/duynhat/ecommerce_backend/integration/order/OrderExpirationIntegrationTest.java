package com.duynhat.ecommerce_backend.integration.order;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.integration.AbstractIntegrationTest;
import com.duynhat.ecommerce_backend.modules.address.ShippingAddressRepository;
import com.duynhat.ecommerce_backend.modules.address.entity.ShippingAddress;
import com.duynhat.ecommerce_backend.modules.cart.CartService;
import com.duynhat.ecommerce_backend.modules.cart.dto.request.AddCartItemRequest;
import com.duynhat.ecommerce_backend.modules.category.CategoryRepository;
import com.duynhat.ecommerce_backend.modules.category.entity.Category;
import com.duynhat.ecommerce_backend.modules.inventory.InventoryTransactionRepository;
import com.duynhat.ecommerce_backend.modules.inventory.enums.InventoryTransactionType;
import com.duynhat.ecommerce_backend.modules.order.OrderRepository;
import com.duynhat.ecommerce_backend.modules.order.OrderService;
import com.duynhat.ecommerce_backend.modules.order.dto.request.CreateOrderRequest;
import com.duynhat.ecommerce_backend.modules.order.dto.response.OrderResponse;
import com.duynhat.ecommerce_backend.modules.order.entity.Order;
import com.duynhat.ecommerce_backend.modules.order.enums.OrderStatus;
import com.duynhat.ecommerce_backend.modules.payment.PaymentRepository;
import com.duynhat.ecommerce_backend.modules.payment.PaymentService;
import com.duynhat.ecommerce_backend.modules.payment.dto.request.CreatePaymentRequest;
import com.duynhat.ecommerce_backend.modules.payment.dto.response.PaymentResponse;
import com.duynhat.ecommerce_backend.modules.payment.enums.PaymentMethod;
import com.duynhat.ecommerce_backend.modules.payment.enums.PaymentStatus;
import com.duynhat.ecommerce_backend.modules.product.ProductRepository;
import com.duynhat.ecommerce_backend.modules.product.entity.Product;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import com.duynhat.ecommerce_backend.modules.user.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "app.order.expiration-initial-delay-ms=3600000",
        "app.order.expiration-scan-delay-ms=3600000"
})
class OrderExpirationIntegrationTest extends AbstractIntegrationTest {

    private static final String USER_EMAIL = "expiration-integration@example.com";

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private ShippingAddressRepository shippingAddressRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User user;
    private Category category;
    private ShippingAddress shippingAddress;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        user = userRepository.saveAndFlush(
                User.builder()
                        .email(USER_EMAIL)
                        .password("test-password")
                        .fullName("Expiration Integration User")
                        .role(Role.USER)
                        .active(true)
                        .build()
        );

        shippingAddress = createShippingAddress(
                user,
                "Expiration Recipient",
                "0901234567",
                "Ha Noi",
                "Cau Giay",
                "Dich Vong",
                "123 Expiration Street"
        );

        category = categoryRepository.saveAndFlush(
                Category.builder()
                        .name("Expiration Test Category")
                        .description("Category for expiration tests")
                        .active(true)
                        .build()
        );

        authenticate(USER_EMAIL);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        cleanDatabase();
    }

    @Test
    void expireOrderIfEligible_whenPendingButNotExpired_shouldDoNothing() {
        Product product = createProduct(
                "Not Expired Product",
                "100.00",
                10
        );

        OrderResponse order = createOrder(product, 3);

        assertThat(getStock(product.getId())).isEqualTo(7);

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);

        boolean expired = orderService
                .expireOrderIfEligible(order.getId(), cutoff);

        assertThat(expired).isFalse();

        assertThat(getOrderStatus(order.getId())).isEqualTo(OrderStatus.PENDING);

        assertThat(getStock(product.getId())).isEqualTo(7);

        assertThat(countExpiredInventoryTransactions()).isZero();
    }

    @Test
    void expireOrderIfEligible_whenPendingAndExpired_shouldSetOrderExpired() {
        Product product = createProduct(
                "Expired Order Product",
                "100.00",
                10
        );

        OrderResponse order = createOrder(product, 2);

        makeOrderOld(order.getId(), 20);

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);

        boolean expired = orderService
                .expireOrderIfEligible(order.getId(), cutoff);

        assertThat(expired).isTrue();

        assertThat(getOrderStatus(order.getId()))
                .isEqualTo(OrderStatus.EXPIRED);
    }

    @Test
    void expireOrderIfEligible_whenExpired_shouldRestoreProductStock() {
        Product product = createProduct(
                "Expiration Stock Product",
                "100.00",
                10
        );

        OrderResponse order = createOrder(product, 3);

        assertThat(getStock(product.getId())).isEqualTo(7);

        makeOrderOld(order.getId(), 20);

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);

        boolean expired = orderService
                .expireOrderIfEligible(order.getId(), cutoff);

        assertThat(expired).isTrue();

        assertThat(getOrderStatus(order.getId()))
                .isEqualTo(OrderStatus.EXPIRED);

        assertThat(getStock(product.getId())).isEqualTo(10);
    }

    @Test
    void expireOrderIfEligible_shouldCreateOrderExpiredInventoryTransaction() {
        Product product = createProduct(
                "Expired Inventory Product",
                "100.00",
                10
        );

        OrderResponse order = createOrder(product, 3);

        makeOrderOld(order.getId(), 20);

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);

        orderService.expireOrderIfEligible(order.getId(), cutoff);

        assertThat(inventoryTransactionRepository
                .findAll()
                .stream()
                .filter(transaction ->
                                transaction.getType()
                                        == InventoryTransactionType.ORDER_EXPIRED)
                .toList()
        )
                .singleElement()
                .satisfies(transaction -> {

                    assertThat(transaction.getQuantityChange()).isEqualTo(3);

                    assertThat(transaction.getStockBefore()).isEqualTo(7);

                    assertThat(transaction.getStockAfter()).isEqualTo(10);

                    assertThat(transaction.getReason()).contains("Order expired:");
                });
    }

    @Test
    void expireOrderIfEligible_whenOrderConfirmed_shouldDoNothing() {
        Product product = createProduct(
                "Confirmed Product",
                "100.00",
                10
        );

        OrderResponse order = createOrder(product, 3);

        Order persistedOrder = orderRepository
                .findById(order.getId())
                .orElseThrow();

        persistedOrder.setStatus(OrderStatus.CONFIRMED);

        orderRepository.saveAndFlush(persistedOrder);

        makeOrderOld(order.getId(), 60);

        boolean expired = orderService
                .expireOrderIfEligible(order.getId(), LocalDateTime.now().minusMinutes(15));

        assertThat(expired).isFalse();

        assertThat(getOrderStatus(order.getId())).isEqualTo(OrderStatus.CONFIRMED);

        assertThat(getStock(product.getId())).isEqualTo(7);

        assertThat(countExpiredInventoryTransactions()).isZero();
    }

    @Test
    void expireOrderIfEligible_whenOrderCancelled_shouldDoNothing() {
        Product product = createProduct(
                "Cancelled Expiration Product",
                "100.00",
                10
        );

        OrderResponse order = createOrder(product, 3);

        assertThat(getStock(product.getId())).isEqualTo(7);

        orderService.cancelMyOrder(order.getId());

        // Cancel đã restore stock.
        assertThat(getStock(product.getId())).isEqualTo(10);

        makeOrderOld(order.getId(), 60);

        boolean expired = orderService.expireOrderIfEligible(
                order.getId(), LocalDateTime.now().minusMinutes(15));

        assertThat(expired).isFalse();

        assertThat(getOrderStatus(order.getId())).isEqualTo(OrderStatus.CANCELLED);

        assertThat(getStock(product.getId())).isEqualTo(10);

        assertThat(countExpiredInventoryTransactions()).isZero();
    }

    @Test
    void expireOrderIfEligible_calledTwice_shouldRestoreStockOnlyOnce() {
        Product product = createProduct(
                "Double Expiration Product",
                "100.00",
                10
        );

        OrderResponse order = createOrder(product, 3);

        assertThat(getStock(product.getId())).isEqualTo(7);

        makeOrderOld(order.getId(), 20);

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);

        boolean firstExpiration = orderService
                .expireOrderIfEligible(order.getId(), cutoff);

        boolean secondExpiration = orderService
                .expireOrderIfEligible(order.getId(), cutoff);

        assertThat(firstExpiration).isTrue();

        assertThat(secondExpiration).isFalse();

        assertThat(getOrderStatus(order.getId()))
                .isEqualTo(OrderStatus.EXPIRED);

        assertThat(getStock(product.getId())).isEqualTo(10);

        assertThat(countExpiredInventoryTransactions()).isEqualTo(1);
    }

    @Test
    void paymentAndExpirationConcurrently_shouldProduceOnlyOneValidOutcome() throws Exception {
        Product product = createProduct(
                "Payment Expiration Race Product",
                "100.00",
                10
        );

        OrderResponse order = createOrder(product, 3);

        assertThat(getStock(product.getId())).isEqualTo(7);

        makeOrderOld(order.getId(), 20);

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);

        CountDownLatch ready = new CountDownLatch(2);

        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<PaymentAttemptResult> paymentFuture =
                    executor.submit(
                            concurrentPaymentTask(
                                    order.getId(),
                                    ready,
                                    start
                            )
                    );

            Future<Boolean> expirationFuture =
                    executor.submit(
                            concurrentExpirationTask(
                                    order.getId(),
                                    cutoff,
                                    ready,
                                    start
                            )
                    );

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();

            start.countDown();

            PaymentAttemptResult paymentResult =
                    paymentFuture.get(10, TimeUnit.SECONDS);

            boolean expired = expirationFuture
                    .get(10, TimeUnit.SECONDS);

            OrderStatus finalStatus = getOrderStatus(order.getId());

            int finalStock = getStock(product.getId());

            if (finalStatus == OrderStatus.CONFIRMED) {
                assertThat(paymentResult.successful()).isTrue();

                assertThat(paymentResult.status()).isEqualTo(PaymentStatus.SUCCESS);

                assertThat(paymentResult.error()).isNull();

                assertThat(expired).isFalse();

                assertThat(finalStock).isEqualTo(7);

                assertThat(paymentRepository.count()).isEqualTo(1);

                assertThat(paymentRepository
                        .findAll()
                        .getFirst()
                        .getStatus()
                ).isEqualTo(PaymentStatus.SUCCESS);

                assertThat(countExpiredInventoryTransactions()).isZero();

                return;
            }

            if (finalStatus == OrderStatus.EXPIRED) {
                assertThat(expired).isTrue();

                assertThat(paymentResult.successful()).isFalse();

                assertThat(paymentResult.error())
                        .isInstanceOf(BadRequestException.class);

                assertThat(finalStock).isEqualTo(10);

                assertThat(paymentRepository.count()).isZero();

                assertThat(countExpiredInventoryTransactions()).isEqualTo(1);

                return;
            }

            throw new AssertionError(
                    "Unexpected final order status: "
                            + finalStatus
            );

        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<PaymentAttemptResult> concurrentPaymentTask(
            UUID orderId,
            CountDownLatch ready,
            CountDownLatch start
    ) {

        return () -> {
            authenticate(USER_EMAIL);

            try {
                ready.countDown();

                boolean started = start.await(5, TimeUnit.SECONDS);

                if (!started) {
                    throw new IllegalStateException("Timed out waiting to start payment");
                }

                try {
                    PaymentResponse response = paymentService
                            .createPayment(
                                    createPaymentRequest(orderId, true),
                                    UUID.randomUUID().toString()
                            );

                    return new PaymentAttemptResult(
                            true,
                            response.getStatus(),
                            null
                    );

                } catch (Exception ex) {
                    return new PaymentAttemptResult(
                            false,
                            null,
                            ex
                    );
                }

            } finally {
                SecurityContextHolder
                        .clearContext();
            }
        };
    }

    private Callable<Boolean> concurrentExpirationTask(
            UUID orderId,
            LocalDateTime cutoff,
            CountDownLatch ready,
            CountDownLatch start
    ) {

        return () -> {
            ready.countDown();

            boolean started = start.await(5, TimeUnit.SECONDS);

            if (!started) {
                throw new IllegalStateException("Timed out waiting to start expiration");
            }

            return orderService.expireOrderIfEligible(orderId, cutoff);
        };
    }

    private Product createProduct(
            String name,
            String price,
            int stock
    ) {

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

    private OrderResponse createOrder(
            Product product,
            int quantity
    ) {
        AddCartItemRequest cartRequest = new AddCartItemRequest();

        cartRequest.setProductId(product.getId());

        cartRequest.setQuantity(quantity);

        cartService.addItem(cartRequest);

        return orderService.createOrder(createOrderRequest());
    }

    private CreateOrderRequest createOrderRequest() {
        CreateOrderRequest request = new CreateOrderRequest();

        request.setAddressId(shippingAddress.getId());

        return request;
    }

    private CreatePaymentRequest createPaymentRequest(
            UUID orderId,
            boolean success
    ) {

        CreatePaymentRequest request = new CreatePaymentRequest();

        request.setOrderId(orderId);

        request.setMethod(PaymentMethod.MOCK);

        request.setSuccess(success);

        return request;
    }

    private ShippingAddress createShippingAddress(
            User owner,
            String recipientName,
            String phoneNumber,
            String province,
            String district,
            String ward,
            String addressLine
    ) {
        ShippingAddress address = new ShippingAddress();

        address.setUser(owner);
        address.setRecipientName(recipientName);
        address.setPhoneNumber(phoneNumber);
        address.setProvince(province);
        address.setDistrict(district);
        address.setWard(ward);
        address.setAddressLine(addressLine);
        address.setDefaultAddress(true);

        return shippingAddressRepository.saveAndFlush(address);
    }

    private void makeOrderOld(
            UUID orderId,
            long minutesAgo
    ) {
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(minutesAgo);

        jdbcTemplate.update(
                """
                UPDATE orders
                SET created_at = ?
                WHERE id = ?
                """,
                Timestamp.valueOf(createdAt),
                orderId
        );
    }

    private OrderStatus getOrderStatus(
            UUID orderId
    ) {
        return orderRepository
                .findById(orderId)
                .orElseThrow()
                .getStatus();
    }

    private int getStock(
            UUID productId
    ) {
        return productRepository
                .findById(productId)
                .orElseThrow()
                .getStock();
    }

    private long countExpiredInventoryTransactions() {
        return inventoryTransactionRepository
                .findAll()
                .stream()
                .filter(transaction ->
                        transaction.getType()
                                == InventoryTransactionType.ORDER_EXPIRED
                )
                .count();
    }

    private void authenticate(
            String email
    ) {
        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of()
                        )
                );
    }

    private void cleanDatabase() {
        jdbcTemplate.execute("""
            TRUNCATE TABLE
                payments,
                inventory_transactions,
                order_items,
                orders,
                cart_items,
                carts,
                shipping_addresses,
                products,
                categories,
                users
            CASCADE
            """);
    }

    private record PaymentAttemptResult(
            boolean successful,
            PaymentStatus status,
            Throwable error
    ) {
    }
}