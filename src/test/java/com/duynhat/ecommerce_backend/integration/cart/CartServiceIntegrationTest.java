package com.duynhat.ecommerce_backend.integration.cart;

import com.duynhat.ecommerce_backend.common.core.exception.ResourceNotFoundException;
import com.duynhat.ecommerce_backend.integration.AbstractIntegrationTest;
import com.duynhat.ecommerce_backend.modules.cart.CartItemRepository;
import com.duynhat.ecommerce_backend.modules.cart.CartRepository;
import com.duynhat.ecommerce_backend.modules.cart.CartService;
import com.duynhat.ecommerce_backend.modules.cart.dto.request.AddCartItemRequest;
import com.duynhat.ecommerce_backend.modules.cart.dto.response.CartResponse;
import com.duynhat.ecommerce_backend.modules.cart.entity.Cart;
import com.duynhat.ecommerce_backend.modules.category.CategoryRepository;
import com.duynhat.ecommerce_backend.modules.category.entity.Category;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class CartServiceIntegrationTest extends AbstractIntegrationTest {

    private static final String USER_EMAIL = "cart-integration@example.com";

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

    private User user;
    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        user = userRepository.saveAndFlush(
                User.builder()
                        .email(USER_EMAIL)
                        .password("test-password")
                        .fullName("Cart Integration User")
                        .role(Role.USER)
                        .active(true)
                        .build()
        );

        category = categoryRepository.saveAndFlush(
                Category.builder()
                        .name("Cart Test Category")
                        .description("Category for cart integration tests")
                        .active(true)
                        .build()
        );

        product = createProduct("Cart Product", "100.00", 10);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        USER_EMAIL,
                        null,
                        List.of()
                )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addItem_withNewProduct_shouldCreateCartItem() {
        CartResponse response = cartService.addItem(addItemRequest(product.getId(), 2));

        Cart cart = getUserCart();
        assertThat(cartItemRepository.findAllByCartId(cart.getId()))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getProduct().getId()).isEqualTo(product.getId());
                    assertThat(item.getQuantity()).isEqualTo(2);
                });

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getTotalItems()).isEqualTo(2);
        assertThat(response.getTotalPrice()).isEqualByComparingTo("200.00");
    }

    @Test
    void addItem_withExistingProduct_shouldIncreaseQuantityWithoutCreatingDuplicate() {
        cartService.addItem(addItemRequest(product.getId(), 2));

        CartResponse response = cartService.addItem(addItemRequest(product.getId(), 3));

        Cart cart = getUserCart();
        assertThat(cartItemRepository.findAllByCartId(cart.getId()))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getProduct().getId()).isEqualTo(product.getId());
                    assertThat(item.getQuantity()).isEqualTo(5);
                });

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getTotalItems()).isEqualTo(5);
        assertThat(response.getTotalPrice()).isEqualByComparingTo("500.00");
    }

    @Test
    void removeItem_withExistingProduct_shouldDeleteRowFromDatabase() {
        cartService.addItem(addItemRequest(product.getId(), 1));
        Cart cart = getUserCart();
        cartItemRepository.flush();
        assertThat(countCartItemsInDatabase(cart.getId())).isEqualTo(1L);

        cartService.removeItem(product.getId());

        assertThat(countCartItemsInDatabase(cart.getId())).isZero();
        assertThat(cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId()))
                .isEmpty();
    }

    @Test
    void clearCart_withMultipleItems_shouldDeleteAllRowsFromDatabase() {
        Product secondProduct = createProduct("Second Cart Product", "50.00", 5);
        cartService.addItem(addItemRequest(product.getId(), 1));
        cartService.addItem(addItemRequest(secondProduct.getId(), 2));

        Cart cart = getUserCart();
        cartItemRepository.flush();
        assertThat(countCartItemsInDatabase(cart.getId())).isEqualTo(2L);

        cartService.clearCart();

        assertThat(countCartItemsInDatabase(cart.getId())).isZero();
        assertThat(cartItemRepository.findAllByCartId(cart.getId())).isEmpty();
        assertThat(cartRepository.existsById(cart.getId())).isTrue();
    }

    @Test
    void getCart_whenExistingProductWasSoftDeleted_shouldMarkItemUnavailable() {
        cartService.addItem(
                addItemRequest(product.getId(), 2)
        );

        product.setDeletedAt(LocalDateTime.now());
        productRepository.saveAndFlush(product);

        CartResponse response = cartService.getCart();

        assertThat(response.getItems())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getProductId()).isEqualTo(product.getId());
                    assertThat(item.getAvailable()).isFalse();
                    assertThat(item.getUnavailableReason()).isEqualTo("Product is no longer available");
                });
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

    private AddCartItemRequest addItemRequest(UUID productId, int quantity) {
        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(productId);
        request.setQuantity(quantity);
        return request;
    }

    private Cart getUserCart() {
        return cartRepository.findByUserId(user.getId()).orElseThrow();
    }

    private long countCartItemsInDatabase(UUID cartId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cart_items WHERE cart_id = ?",
                Long.class,
                cartId
        );
        return count == null ? 0 : count;
    }
}
