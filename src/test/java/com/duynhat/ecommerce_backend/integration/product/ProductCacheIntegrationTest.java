package com.duynhat.ecommerce_backend.integration.product;

import com.duynhat.ecommerce_backend.common.core.exception.ResourceNotFoundException;
import com.duynhat.ecommerce_backend.integration.AbstractIntegrationTest;
import com.duynhat.ecommerce_backend.modules.address.ShippingAddressRepository;
import com.duynhat.ecommerce_backend.modules.address.entity.ShippingAddress;
import com.duynhat.ecommerce_backend.modules.cart.CartService;
import com.duynhat.ecommerce_backend.modules.cart.dto.request.AddCartItemRequest;
import com.duynhat.ecommerce_backend.modules.category.CategoryRepository;
import com.duynhat.ecommerce_backend.modules.category.CategoryService;
import com.duynhat.ecommerce_backend.modules.category.dto.request.UpdateCategoryRequest;
import com.duynhat.ecommerce_backend.modules.category.entity.Category;
import com.duynhat.ecommerce_backend.modules.order.OrderService;
import com.duynhat.ecommerce_backend.modules.order.dto.request.CreateOrderRequest;
import com.duynhat.ecommerce_backend.modules.order.dto.response.OrderResponse;
import com.duynhat.ecommerce_backend.modules.order.enums.OrderStatus;
import com.duynhat.ecommerce_backend.modules.product.ProductRepository;
import com.duynhat.ecommerce_backend.modules.product.ProductService;
import com.duynhat.ecommerce_backend.modules.product.dto.request.AdjustProductStockRequest;
import com.duynhat.ecommerce_backend.modules.product.dto.request.UpdateProductRequest;
import com.duynhat.ecommerce_backend.modules.product.dto.response.ProductResponse;
import com.duynhat.ecommerce_backend.modules.product.entity.Product;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import com.duynhat.ecommerce_backend.modules.user.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.duynhat.ecommerce_backend.config.RedisCacheConfig.PRODUCT_DETAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestPropertySource(properties = {
        "app.order.expiration-initial-delay-ms=3600000",
        "app.order.expiration-scan-delay-ms=3600000"
})
class ProductCacheIntegrationTest
        extends AbstractIntegrationTest {

    private static final String USER_EMAIL =
            "product-cache-user@example.com";

    private static final String ADMIN_EMAIL =
            "product-cache-admin@example.com";

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ShippingAddressRepository shippingAddressRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User user;
    private User admin;
    private Category category;
    private ShippingAddress shippingAddress;

    @BeforeEach
    void setUp() {

        clearProductCache();

        cleanDatabase();

        user = userRepository.saveAndFlush(
                User.builder()
                        .email(USER_EMAIL)
                        .password("test-password")
                        .fullName("Product Cache User")
                        .role(Role.USER)
                        .active(true)
                        .build()
        );

        admin = userRepository.saveAndFlush(
                User.builder()
                        .email(ADMIN_EMAIL)
                        .password("test-password")
                        .fullName("Product Cache Admin")
                        .role(Role.ADMIN)
                        .active(true)
                        .build()
        );

        category = categoryRepository.saveAndFlush(
                Category.builder()
                        .name("Cache Test Category")
                        .description("Category for cache test")
                        .active(true)
                        .build()
        );

        shippingAddress = createShippingAddress(user);

        authenticate(USER_EMAIL);
    }

    @AfterEach
    void tearDown() {

        SecurityContextHolder.clearContext();

        clearProductCache();

        cleanDatabase();
    }

    @Test
    void getById_secondCall_shouldUseCachedValue() {

        Product product =
                createProduct(
                        "Cached Product",
                        "100.00",
                        10
                );

        ProductResponse first = productService.getById(product.getId());

        assertThat(
                first.getDescription()
        ).isEqualTo("Cached Product description");

        jdbcTemplate.update(
                """
                UPDATE products
                SET description = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                "Changed directly in database",
                product.getId()
        );

        String dbDescription =
                jdbcTemplate.queryForObject(
                        """
                        SELECT description
                        FROM products
                        WHERE id = ?
                        """,
                        String.class,
                        product.getId()
                );

        assertThat(dbDescription).isEqualTo("Changed directly in database");

        ProductResponse second = productService.getById(product.getId());

        assertThat(
                second.getDescription()
        ).isEqualTo("Cached Product description");
    }

    @Test
    void update_shouldEvictProductDetailCache() {
        Product product = createProduct(
                "Old Product",
                "100.00",
                10
        );

        ProductResponse cached = productService.getById(product.getId());

        assertThat(cached.getName()).isEqualTo("Old Product");

        UpdateProductRequest request = new UpdateProductRequest();

        request.setName("Updated Product");

        request.setDescription("Updated description");

        request.setPrice(new BigDecimal("150.00"));

        request.setCategoryName(category.getName());

        request.setActive(true);

        productService.update(product.getId(), request);

        ProductResponse afterUpdate = productService.getById(product.getId());

        assertThat(
                afterUpdate.getName()
        ).isEqualTo("Updated Product");

        assertThat(
                afterUpdate.getDescription()
        ).isEqualTo("Updated description");

        assertThat(
                afterUpdate.getPrice()
        ).isEqualByComparingTo("150.00");
    }

    @Test
    void adjustStock_shouldEvictProductDetailCache() {
        Product product = createProduct(
                "Stock Product",
                "100.00",
                10
        );

        ProductResponse cached = productService.getById(product.getId());

        assertThat(cached.getStock()).isEqualTo(10);

        authenticate(ADMIN_EMAIL);

        AdjustProductStockRequest request = new AdjustProductStockRequest();

        request.setQuantity(5);

        request.setReason("Cache integration test");

        productService.adjustStock(product.getId(), request);

        ProductResponse afterAdjustment = productService.getById(product.getId());

        assertThat(afterAdjustment.getStock()).isEqualTo(15);
    }

    @Test
    void delete_shouldEvictProductDetailCache() {
        Product product = createProduct(
                "Delete Product",
                "100.00",
                10
        );

        ProductResponse cached = productService.getById(product.getId());

        assertThat(cached.getId()).isEqualTo(product.getId());

        productService.delete(product.getId());

        assertThatThrownBy(
                () -> productService.getById(product.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(productRepository
                .findById(product.getId())
                .orElseThrow()
                .getDeletedAt()
        ).isNotNull();
    }

    @Test
    void createOrder_shouldEvictProductCache_afterStockDecrease() {
        Product product = createProduct(
                "Order Product",
                "100.00",
                10
        );

        ProductResponse beforeOrder = productService.getById(product.getId());

        assertThat(beforeOrder.getStock()).isEqualTo(10);

        addToCart(product, 3);

        OrderResponse order = orderService
                .createOrder(createOrderRequest());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);

        assertThat(getStockFromDatabase(product.getId())).isEqualTo(7);

        ProductResponse afterOrder = productService.getById(product.getId());

        assertThat(afterOrder.getStock()).isEqualTo(7);
    }

    @Test
    void cancelOrder_shouldEvictProductCache_afterStockRestore() {
        Product product = createProduct(
                "Cancel Product",
                "100.00",
                10
        );

        addToCart(product, 3);

        OrderResponse order = orderService.createOrder(createOrderRequest());

        ProductResponse cachedAfterOrder = productService.getById(product.getId());

        assertThat(cachedAfterOrder.getStock()).isEqualTo(7);

        OrderResponse cancelled = orderService.cancelMyOrder(order.getId());

        assertThat(
                cancelled.getStatus()
        ).isEqualTo(OrderStatus.CANCELLED);

        assertThat(
                getStockFromDatabase(product.getId())
        ).isEqualTo(10);

        ProductResponse afterCancel = productService.getById(product.getId());

        assertThat(afterCancel.getStock()).isEqualTo(10);
    }

    @Test
    void expireOrder_shouldEvictProductCache_afterStockRestore() {
        Product product = createProduct(
                "Expired Product",
                "100.00",
                10
        );

        addToCart(product, 4);

        OrderResponse order = orderService.createOrder(createOrderRequest());

        ProductResponse cachedAfterOrder = productService.getById(product.getId());

        assertThat(cachedAfterOrder.getStock()).isEqualTo(6);

        makeOrderOld(order.getId(), 20);

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);

        boolean expired = orderService
                .expireOrderIfEligible(order.getId(), cutoff);

        assertThat(expired).isTrue();

        assertThat(
                getStockFromDatabase(product.getId())
        ).isEqualTo(10);

        ProductResponse afterExpiration =
                productService.getById(product.getId());

        assertThat(
                afterExpiration.getStock()
        ).isEqualTo(10);
    }

    @Test
    void createOrder_withMultipleProducts_shouldEvictAllAffectedProductCaches() {

        Product first = createProduct(
                "First Product",
                "100.00",
                10
        );

        Product second = createProduct(
                "Second Product",
                "50.00",
                20
        );

        assertThat(
                productService.getById(first.getId()).getStock()
        ).isEqualTo(10);

        assertThat(
                productService.getById(second.getId()).getStock()
        ).isEqualTo(20);

        addToCart(first, 2);

        addToCart(second, 5);

        orderService.createOrder(createOrderRequest());

        assertThat(
                productService.getById(first.getId()).getStock()
        ).isEqualTo(8);

        assertThat(
                productService.getById(second.getId()).getStock()
        ).isEqualTo(15);
    }

    @Test
    void updateCategoryName_shouldEvictAffectedProductCaches() {
        Product product = createProduct(
                "Category Cache Product",
                "100.00",
                10
        );

        ProductResponse cached = productService.getById(product.getId());

        assertThat(
                cached.getCategoryName()
        ).isEqualTo("Cache Test Category");

        UpdateCategoryRequest request = new UpdateCategoryRequest();

        request.setName("Updated Category");

        request.setDescription("Updated description");

        request.setActive(true);

        categoryService.update(category.getId(), request);

        ProductResponse afterUpdate =
                productService.getById(product.getId());

        assertThat(
                afterUpdate.getCategoryName()
        ).isEqualTo("Updated Category");
    }

    @Test
    void deactivateCategory_shouldEvictProductCache_andHideProductFromPublic() {
        Product product = createProduct(
                "Inactive Category Product",
                "100.00",
                10
        );

        ProductResponse cached = productService.getById(product.getId());

        assertThat(cached.getId())
                .isEqualTo(product.getId());

        UpdateCategoryRequest request = new UpdateCategoryRequest();

        request.setName(category.getName());

        request.setDescription(category.getDescription());

        request.setActive(false);

        categoryService.update(category.getId(), request);

        assertThatThrownBy(
                () -> productService.getById(product.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Product createProduct(
            String name,
            String price,
            int stock
    ) {
        return productRepository
                .saveAndFlush(Product.builder()
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

        request.setAddressId(shippingAddress.getId());

        return request;
    }

    private ShippingAddress createShippingAddress(User owner) {
        ShippingAddress address = new ShippingAddress();

        address.setUser(owner);

        address.setRecipientName("Cache Test Recipient");

        address.setPhoneNumber("0901234567");

        address.setProvince("Ha Noi");

        address.setDistrict("Cau Giay");

        address.setWard("Dich Vong");

        address.setAddressLine("123 Cache Test Street");

        address.setDefaultAddress(true);

        return shippingAddressRepository.saveAndFlush(address);
    }

    private int getStockFromDatabase(UUID productId) {

        Integer stock =
                jdbcTemplate.queryForObject(
                        """
                        SELECT stock
                        FROM products
                        WHERE id = ?
                        """,
                        Integer.class,
                        productId
                );

        if (stock == null) {
            throw new IllegalStateException("Product stock not found");
        }

        return stock;
    }

    private void makeOrderOld(UUID orderId, long minutesAgo) {
        LocalDateTime createdAt = LocalDateTime.now()
                .minusMinutes(minutesAgo);

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

    private void clearProductCache() {
        Cache cache = cacheManager.getCache(PRODUCT_DETAIL);

        if (cache != null) {
            cache.clear();
        }
    }

    private void cleanDatabase() {
        jdbcTemplate.execute(
                """
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
                """
        );
    }
}