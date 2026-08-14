package com.duynhat.ecommerce_backend.modules.cart.impl;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.common.core.exception.ResourceNotFoundException;
import com.duynhat.ecommerce_backend.modules.cart.CartItemRepository;
import com.duynhat.ecommerce_backend.modules.cart.CartRepository;
import com.duynhat.ecommerce_backend.modules.cart.CartService;
import com.duynhat.ecommerce_backend.modules.cart.dto.request.AddCartItemRequest;
import com.duynhat.ecommerce_backend.modules.cart.dto.request.UpdateCartItemRequest;
import com.duynhat.ecommerce_backend.modules.cart.dto.response.CartItemResponse;
import com.duynhat.ecommerce_backend.modules.cart.dto.response.CartResponse;
import com.duynhat.ecommerce_backend.modules.cart.entity.Cart;
import com.duynhat.ecommerce_backend.modules.cart.entity.CartItem;
import com.duynhat.ecommerce_backend.modules.product.ProductRepository;
import com.duynhat.ecommerce_backend.modules.product.entity.Product;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public CartResponse getCart() {
        User user = getCurrentUser();

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElse(null);

        if (cart == null) {
            return emptyCartResponse();
        }

        return  toResponse(cart);
    }

    @Override
    public  CartResponse addItem(AddCartItemRequest req) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCartForUpdate(user);

        Product product = productRepository
                .findByIdAndDeletedAtIsNull(req.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        validateProductAvailable(product);

        CartItem item = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        int newQuantity = req.getQuantity();

        if (item != null) {
            newQuantity += item.getQuantity();
        }

        validateStock(product, newQuantity);

        if (item == null) {
            item = new CartItem();
            item.setProduct(product);
            cart.addItem(item);
        }

        item.setQuantity(newQuantity);

        cartItemRepository.save(item);

        return getCartResponseByUserId(user.getId());
    }

    @Override
    public CartResponse updateItem(UUID productId, UpdateCartItemRequest req) {
        User user = getCurrentUser();

        Cart cart = cartRepository
                .findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        CartItem item = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        Product product = item.getProduct();

        validateProductAvailable(product);
        validateStock(product, req.getQuantity());

        item.setQuantity(req.getQuantity());

        cartItemRepository.save(item);

        return getCartResponseByUserId(user.getId());
    }

    @Override
    public void removeItem(UUID productId) {
        User user = getCurrentUser();

        Cart cart = cartRepository
                .findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        int deletedRows = cartItemRepository.deleteByCartIdAndProductId(
                cart.getId(),
                productId
        );

        if (deletedRows == 0) {
            throw new ResourceNotFoundException("Cart item not found");
        }
    }

    @Override
    public void clearCart() {
        User user = getCurrentUser();

        cartRepository.findByUserIdForUpdate(user.getId())
                .ifPresent(cart ->
                        cartItemRepository.deleteAllByCartId(cart.getId()));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BadRequestException("User is not authenticated");
        }

        String email = authentication.getName();

        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Cart getOrCreateCartForUpdate(User user) {
        Optional<Cart> existingCart = cartRepository.findByUserIdForUpdate(user.getId());

        if (existingCart.isPresent()) {
            return existingCart.get();
        }

        User lockedUser = userRepository
                .findByIdForUpdate(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return cartRepository
                .findByUserIdForUpdate(user.getId())
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(lockedUser);

                    return cartRepository.save(cart);
                });
    }

    private void validateProductAvailable(Product product) {
        if (product.getDeletedAt() != null) {
            throw new BadRequestException("Product is not available");
        }

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new BadRequestException("Product is not available");
        }

        if (product.getCategory().getDeletedAt() != null) {
            throw new BadRequestException("Product category is not available");
        }

        if (!Boolean.TRUE.equals(product.getCategory().getActive())) {
            throw new BadRequestException("Product category is not available");
        }

        if (product.getStock() == null || product.getStock() <= 0) {
            throw new BadRequestException("Product is out of stock");
        }
    }

    private void validateStock(Product product, int quantity) {
        if (quantity > product.getStock()) {
            throw new BadRequestException("Requested quantity exceeds available stock");
        }
    }

    private CartResponse emptyCartResponse() {
        return CartResponse.builder()
                .cartId(null)
                .items(List.of())
                .totalItems(0)
                .totalPrice(BigDecimal.ZERO)
                .build();
    }

    private CartResponse getCartResponseByUserId(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        return toResponse(cart);
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems()
                .stream()
                .map(this::toItemResponse)
                .toList();

        int totalItems = cart.getItems()
                .stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        BigDecimal totalPrice = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(items)
                .totalItems(totalItems)
                .totalPrice(totalPrice)
                .build();
    }

    private CartItemResponse toItemResponse (CartItem item) {
        Product product = item.getProduct();

        BigDecimal subtotal = product.getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        boolean available = isProductAvailable(
                product,
                item.getQuantity()
        );

        String unavailableReason = getUnavailableReason(
                product,
                item.getQuantity()
        );

        return CartItemResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .imageUrl(product.getImageUrl())
                .unitPrice(product.getPrice())
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .availableStock(product.getStock())
                .available(available)
                .unavailableReason(unavailableReason)
                .build();
    }

    private boolean isProductAvailable(Product product, int quantity) {
        return product.getDeletedAt() == null
                && Boolean.TRUE.equals(product.getActive())
                && product.getCategory().getDeletedAt() == null
                && Boolean.TRUE.equals(product.getCategory().getActive())
                && product.getStock() != null
                && product.getStock() >= quantity;
    }

    private String getUnavailableReason(Product product, int quantity) {
        if (product.getDeletedAt() != null) {
            return "Product is no longer available";
        }

        if (!Boolean.TRUE.equals(product.getActive())) {
            return "Product is inactive";
        }

        if (product.getCategory().getDeletedAt() != null) {
            return "Product category is no longer available";
        }

        if (!Boolean.TRUE.equals(product.getCategory().getActive())) {
            return "Product category is inactive";
        }

        if (product.getStock() == null || product.getStock() <= 0) {
            return "Product is out of stock";
        }

        if (product.getStock() < quantity) {
            return "Insufficient stock";
        }

        return null;
    }
}
