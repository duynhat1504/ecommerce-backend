package com.duynhat.ecommerce_backend.integration.payment;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.common.core.exception.ResourceNotFoundException;
import com.duynhat.ecommerce_backend.integration.AbstractIntegrationTest;
import com.duynhat.ecommerce_backend.modules.address.ShippingAddressRepository;
import com.duynhat.ecommerce_backend.modules.address.entity.ShippingAddress;
import com.duynhat.ecommerce_backend.modules.cart.CartService;
import com.duynhat.ecommerce_backend.modules.cart.dto.request.AddCartItemRequest;
import com.duynhat.ecommerce_backend.modules.category.CategoryRepository;
import com.duynhat.ecommerce_backend.modules.category.entity.Category;
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
import com.duynhat.ecommerce_backend.modules.payment.entity.Payment;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class PaymentIntegrationTest extends AbstractIntegrationTest {

    private static final String USER_EMAIL = "payment-integration@example.com";

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

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
                        .fullName("Payment Integration User")
                        .role(Role.USER)
                        .active(true)
                        .build()
        );

        shippingAddress = createShippingAddress(
                user,
                "Payment Recipient",
                "0901234567",
                "Ha Noi",
                "Cau Giay",
                "Dich Vong",
                "123 Payment Street"
        );

        category = categoryRepository.saveAndFlush(
                Category.builder()
                        .name("Payment Test Category")
                        .description("Category for payment tests")
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
    void createPayment_success_shouldConfirmOrder() {
        OrderResponse order = createOrder(
                "Payment Success Product",
                "100.00",
                2
        );

        String key = UUID.randomUUID().toString();

        PaymentResponse payment = paymentService.createPayment(
                createPaymentRequest(order.getId(), true),
                key
        );

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        assertThat(payment.getOrderId()).isEqualTo(order.getId());

        assertThat(payment.getAmount()).isEqualByComparingTo("200.00");

        assertThat(payment.getMethod()).isEqualTo(PaymentMethod.MOCK);

        assertThat(payment.getTransactionCode()).isNotBlank();

        assertThat(payment.getFailureReason()).isNull();

        assertThat(orderRepository
                .findById(order.getId())
                .orElseThrow()
                .getStatus()
        ).isEqualTo(OrderStatus.CONFIRMED);

        assertThat(paymentRepository.count())
                .isEqualTo(1);
    }

    @Test
    void createPayment_sameIdempotencyKeyForSameOrder_shouldReturnExistingPayment() {
        OrderResponse order = createOrder(
                "Idempotent Product",
                "100.00",
                1
        );

        String key = UUID.randomUUID().toString();

        CreatePaymentRequest request =
                createPaymentRequest(order.getId(), true);

        PaymentResponse first = paymentService.createPayment(
                request,
                key
        );

        PaymentResponse second = paymentService.createPayment(
                request,
                key
        );

        assertThat(second.getId()).isEqualTo(first.getId());

        assertThat(second.getOrderId()).isEqualTo(first.getOrderId());

        assertThat(second.getTransactionCode()).isEqualTo(first.getTransactionCode());

        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    @Test
    void createPayment_sameIdempotencyKeyForDifferentOrder_shouldReject() {
        OrderResponse firstOrder = createOrder(
                "First Payment Product",
                "100.00",
                1
        );

        OrderResponse secondOrder = createOrder(
                "Second Payment Product",
                "200.00",
                1
        );

        String key = UUID.randomUUID().toString();

        paymentService.createPayment(
                createPaymentRequest(
                        firstOrder.getId(),
                        false
                ),
                key
        );

        assertThatThrownBy(
                () -> paymentService.createPayment(
                        createPaymentRequest(secondOrder.getId(), true),
                        key
                )
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Idempotency-Key has already been used for another order");

        assertThat(paymentRepository.count()).isEqualTo(1);

        assertThat(orderRepository
                .findById(secondOrder.getId())
                .orElseThrow()
                .getStatus()
        ).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void createPayment_failed_shouldKeepOrderPending() {
        OrderResponse order = createOrder(
                "Failed Payment Product",
                "150.00",
                2
        );

        PaymentResponse payment =
                paymentService.createPayment(
                        createPaymentRequest(
                                order.getId(),
                                false
                        ),
                        UUID.randomUUID().toString()
                );

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);

        assertThat(payment.getTransactionCode()).isNull();

        assertThat(payment.getFailureReason()).isEqualTo("Mock payment failed");

        assertThat(
                orderRepository
                        .findById(order.getId())
                        .orElseThrow()
                        .getStatus()
        ).isEqualTo(OrderStatus.PENDING);

        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    @Test
    void createPayment_failedThenRetrySuccess_shouldCreateTwoPaymentAttempts() {
        OrderResponse order = createOrder(
                "Retry Payment Product",
                "250.00",
                1
        );

        PaymentResponse failed =
                paymentService.createPayment(
                        createPaymentRequest(
                                order.getId(),
                                false
                        ),
                        UUID.randomUUID().toString()
                );

        PaymentResponse success =
                paymentService.createPayment(
                        createPaymentRequest(
                                order.getId(),
                                true
                        ),
                        UUID.randomUUID().toString()
                );

        assertThat(failed.getStatus()).isEqualTo(PaymentStatus.FAILED);

        assertThat(success.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        assertThat(success.getId()).isNotEqualTo(failed.getId());

        assertThat(paymentRepository.count()).isEqualTo(2);

        assertThat(paymentRepository
                .findAllByOrderIdOrderByCreatedAtDesc(order.getId())
        ).hasSize(2);

        assertThat(
                orderRepository
                        .findById(order.getId())
                        .orElseThrow()
                        .getStatus()
        ).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void createPayment_forAnotherUsersOrder_shouldReturnNotFoundAndNotCreatePayment() {
        OrderResponse order = createOrder(
                "Ownership Product",
                "100.00",
                1
        );

        User anotherUser = userRepository.saveAndFlush(
                User.builder()
                        .email("another-payment-user@example.com")
                        .password("test-password")
                        .fullName("Another Payment User")
                        .role(Role.USER)
                        .active(true)
                        .build()
        );

        authenticate(anotherUser.getEmail());

        assertThatThrownBy(
                () -> paymentService.createPayment(
                        createPaymentRequest(order.getId(), true),
                        UUID.randomUUID().toString()
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found");

        assertThat(paymentRepository.count()).isZero();

        Order persistedOrder = orderRepository
                .findById(order.getId())
                .orElseThrow();

        assertThat(persistedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void createPayment_whenOrderAlreadyPaid_shouldRejectAndNotCreateSecondPayment() {
        OrderResponse order = createOrder(
                "Already Paid Product",
                "200.00",
                1
        );

        String firstKey = UUID.randomUUID().toString();

        PaymentResponse firstPayment =
                paymentService.createPayment(
                        createPaymentRequest(order.getId(), true),
                        firstKey
                );

        assertThat(firstPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        assertThat(
                orderRepository
                        .findById(order.getId())
                        .orElseThrow()
                        .getStatus()
        ).isEqualTo(OrderStatus.CONFIRMED);

        String secondKey = UUID.randomUUID().toString();

        assertThatThrownBy(
                () -> paymentService.createPayment(
                        createPaymentRequest(order.getId(), true),
                        secondKey
                )
        )
                .isInstanceOf(BadRequestException.class);

        assertThat(paymentRepository.count())
                .isEqualTo(1);

        List<Payment> payments = paymentRepository
                .findAllByOrderIdOrderByCreatedAtDesc(order.getId());

        assertThat(payments)
                .singleElement()
                .satisfies(payment -> {
                    assertThat(payment.getStatus())
                            .isEqualTo(PaymentStatus.SUCCESS);

                    assertThat(payment.getId())
                            .isEqualTo(firstPayment.getId());
                });

        assertThat(
                orderRepository
                        .findById(order.getId())
                        .orElseThrow()
                        .getStatus()
        ).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void createPayment_success_shouldNotDecreaseProductStockAgain() {
        Product product = productRepository.saveAndFlush(
                Product.builder()
                        .name("Payment Stock Product")
                        .description("Stock invariant test")
                        .price(new BigDecimal("100.00"))
                        .stock(10)
                        .active(true)
                        .category(category)
                        .build()
        );

        AddCartItemRequest cartRequest = new AddCartItemRequest();

        cartRequest.setProductId(product.getId());
        cartRequest.setQuantity(3);

        cartService.addItem(cartRequest);

        OrderResponse order = orderService.createOrder(createOrderRequest());

        assertThat(productRepository
                .findById(product.getId())
                .orElseThrow()
                .getStock()
        ).isEqualTo(7);

        paymentService.createPayment(
                createPaymentRequest(order.getId(), true),
                UUID.randomUUID().toString()
        );

        assertThat(productRepository
                .findById(product.getId())
                .orElseThrow()
                .getStock()
        ).isEqualTo(7);
    }

    @Test
    void createPayment_whenOrderCancelled_shouldRejectAndNotCreatePayment() {
        OrderResponse order = createOrder(
                "Cancelled Payment Product",
                "100.00",
                1
        );

        orderService.cancelMyOrder(order.getId());

        assertThat(orderRepository
                .findById(order.getId())
                .orElseThrow()
                .getStatus()
        ).isEqualTo(OrderStatus.CANCELLED);

        assertThatThrownBy(
                () -> paymentService.createPayment(
                        createPaymentRequest(order.getId(), true),
                        UUID.randomUUID().toString()
                )
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cancelled order cannot be paid");

        assertThat(paymentRepository.count()).isZero();
    }

    @Test
    void createPayment_concurrentlyWithSameIdempotencyKey_shouldCreateOnlyOnePayment()
            throws Exception {

        OrderResponse order = createOrder(
                "Concurrent Same Key Product",
                "100.00",
                1
        );

        String idempotencyKey = UUID.randomUUID().toString();

        CountDownLatch ready = new CountDownLatch(2);

        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Callable<PaymentResponse> task =
                    concurrentPaymentTask(
                            order.getId(),
                            idempotencyKey,
                            true,
                            ready,
                            start
                    );

            Future<PaymentResponse> firstFuture = executor.submit(task);

            Future<PaymentResponse> secondFuture = executor.submit(task);

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();

            // Cho cả hai cùng chạy.
            start.countDown();

            PaymentResponse first = firstFuture.get(10, TimeUnit.SECONDS);

            PaymentResponse second = secondFuture.get(10, TimeUnit.SECONDS);

            assertThat(second.getId()).isEqualTo(first.getId());

            assertThat(second.getOrderId()).isEqualTo(first.getOrderId());

            assertThat(second.getTransactionCode()).isEqualTo(first.getTransactionCode());

            assertThat(first.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

            assertThat(second.getStatus()).isEqualTo(PaymentStatus.SUCCESS);


            assertThat(paymentRepository.count()).isEqualTo(1);

            assertThat(paymentRepository
                    .findAllByOrderIdOrderByCreatedAtDesc(order.getId())
            )
                    .singleElement()
                    .satisfies(payment -> {
                        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

                        assertThat(payment.getId()).isEqualTo(first.getId());
                    });

            assertThat(orderRepository
                    .findById(order.getId())
                    .orElseThrow()
                    .getStatus()
            ).isEqualTo(OrderStatus.CONFIRMED);

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void createPayment_concurrentlyWithDifferentKeys_shouldAllowOnlyOneSuccessfulPayment()
            throws Exception {

        OrderResponse order = createOrder(
                "Concurrent Different Key Product",
                "150.00",
                1
        );

        String firstKey = UUID.randomUUID().toString();

        String secondKey = UUID.randomUUID().toString();

        CountDownLatch ready = new CountDownLatch(2);

        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<PaymentResponse> firstFuture =
                    executor.submit(concurrentPaymentTask(
                            order.getId(),
                            firstKey,
                            true,
                            ready,
                            start)
                    );

            Future<PaymentResponse> secondFuture =
                    executor.submit(concurrentPaymentTask(
                            order.getId(),
                            secondKey,
                            true,
                            ready,
                            start)
                    );

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();

            start.countDown();

            int successCount = 0;
            int rejectedCount = 0;

            Future<PaymentResponse>[] futures = new Future[]{firstFuture, secondFuture};

            for (Future<PaymentResponse> future : futures) {
                try {
                    PaymentResponse response = future.get(10, TimeUnit.SECONDS);

                    assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

                    successCount++;

                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();

                    assertThat(cause)
                            .isInstanceOf(BadRequestException.class);

                    rejectedCount++;
                }
            }

            assertThat(successCount).isEqualTo(1);

            assertThat(rejectedCount).isEqualTo(1);

            List<Payment> payments = paymentRepository
                    .findAllByOrderIdOrderByCreatedAtDesc(order.getId());

            assertThat(payments).hasSize(1);

            assertThat(payments)
                    .singleElement()
                    .satisfies(payment ->
                            assertThat(payment.getStatus())
                                    .isEqualTo(PaymentStatus.SUCCESS)
                    );

            assertThat(orderRepository
                    .findById(order.getId())
                    .orElseThrow()
                    .getStatus()
            ).isEqualTo(OrderStatus.CONFIRMED);

        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<PaymentResponse> concurrentPaymentTask(
            UUID orderId,
            String idempotencyKey,
            boolean success,
            CountDownLatch ready,
            CountDownLatch start
    ) {

        return () -> {
            authenticate(USER_EMAIL);

            try {
                ready.countDown();

                boolean started = start.await(5, TimeUnit.SECONDS);

                if (!started) {
                    throw new IllegalStateException("Timed out waiting to start concurrent payment");
                }

                return paymentService.createPayment(
                        createPaymentRequest(orderId, success),
                        idempotencyKey
                );

            } finally {
                SecurityContextHolder.clearContext();
            }
        };
    }

    private OrderResponse createOrder(
            String productName,
            String price,
            int quantity
    ) {
        Product product =
                productRepository.saveAndFlush(Product.builder()
                        .name(productName)
                        .description(productName + " description")
                        .price(new BigDecimal(price))
                        .stock(10)
                        .active(true)
                        .category(category)
                        .build()
                );

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

    private void authenticate(String email) {
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

        return shippingAddressRepository
                .saveAndFlush(address);
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
}
