package com.duynhat.ecommerce_backend.modules.cart;

import com.duynhat.ecommerce_backend.modules.cart.dto.request.AddCartItemRequest;
import com.duynhat.ecommerce_backend.modules.cart.dto.request.UpdateCartItemRequest;
import com.duynhat.ecommerce_backend.modules.cart.dto.response.CartResponse;

import java.util.UUID;

public interface CartService {

    CartResponse getCart();
    CartResponse addItem(AddCartItemRequest request);
    CartResponse updateItem(UUID productId, UpdateCartItemRequest request);
    void removeItem(UUID productId);
    void clearCart();
}
