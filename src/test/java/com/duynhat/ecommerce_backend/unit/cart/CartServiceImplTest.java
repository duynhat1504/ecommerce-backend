package com.duynhat.ecommerce_backend.unit.cart;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.common.core.exception.ResourceNotFoundException;
import com.duynhat.ecommerce_backend.modules.cart.CartItemRepository;
import com.duynhat.ecommerce_backend.modules.cart.CartRepository;
import com.duynhat.ecommerce_backend.modules.cart.dto.request.AddCartItemRequest;
import com.duynhat.ecommerce_backend.modules.cart.entity.Cart;
import com.duynhat.ecommerce_backend.modules.cart.impl.CartServiceImpl;
import com.duynhat.ecommerce_backend.modules.product.ProductRepository;
import com.duynhat.ecommerce_backend.modules.product.entity.Product;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private User user;
    private Cart cart;
    private Product product;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("usertest@gmail.com");

        cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setUser(user);

        product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Macbook Air M2");
        product.setActive(true);
        product.setStock(5);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "usertest@gmail.com",
                        null,
                        List.of()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addItem_whenQuantityExceedsStock_shouldThrowBadRequest() {
        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(product.getId());
        request.setQuantity(6);

        when(userRepository.findByEmailIgnoreCase("usertest@gmail.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(cart));

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Requested quantity exceeds available stock");

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItem_whenProductInactive_shouldThrowBadRequest() {
        product.setActive(false);

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(product.getId());
        request.setQuantity(1);

        when(userRepository.findByEmailIgnoreCase("usertest@gmail.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(cart));

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Product is not available");

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void productStock_shouldRemainUnchanged_whenAddItemFails() {
        int originalStock = product.getStock();

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(product.getId());
        request.setQuantity(100);

        when(userRepository.findByEmailIgnoreCase("usertest@gmail.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(cart));

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId()))
                .thenReturn(Optional.empty());

        try {
            cartService.addItem(request);
        } catch (BadRequestException ignored) {
        }

        assertThat(product.getStock()).isEqualTo(originalStock);
    }

    @Test
    void addItem_whenProductMissing_shouldThrowNotFound() {
        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(product.getId());
        request.setQuantity(1);

        when(userRepository.findByEmailIgnoreCase("usertest@gmail.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(productRepository.findById(product.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found");

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItem_whenProductOutOfStock_shouldThrowBadRequest() {
        product.setStock(0);
        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(product.getId());
        request.setQuantity(1);

        when(userRepository.findByEmailIgnoreCase("usertest@gmail.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Product is out of stock");

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void removeItem_whenItemMissing_shouldThrowNotFound() {
        when(userRepository.findByEmailIgnoreCase("usertest@gmail.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.deleteByCartIdAndProductId(cart.getId(), product.getId())).thenReturn(0);

        assertThatThrownBy(() -> cartService.removeItem(product.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Cart item not found");
    }

    @Test
    void clearCart_whenCartMissing_shouldDoNothing() {
        when(userRepository.findByEmailIgnoreCase("usertest@gmail.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        cartService.clearCart();

        verify(cartItemRepository, never()).deleteAllByCartId(any());
    }

    @Test
    void getCart_whenUnauthenticated_shouldThrowBadRequest() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> cartService.getCart())
                .isInstanceOf(BadRequestException.class)
                .hasMessage("User is not authenticated");

        verifyNoInteractions(userRepository, cartRepository);
    }
}
