package com.duynhat.ecommerce_backend.modules.cart;

import com.duynhat.ecommerce_backend.common.core.dto.ApiResponse;
import com.duynhat.ecommerce_backend.modules.cart.dto.request.AddCartItemRequest;
import com.duynhat.ecommerce_backend.modules.cart.dto.request.UpdateCartItemRequest;
import com.duynhat.ecommerce_backend.modules.cart.dto.response.CartResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "Shopping cart APIs")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    @Operation(summary = "Get current user's cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        CartResponse cart = cartService.getCart();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get cart successfully",
                        cart
                )
        );
    }

    @PostMapping("/items")
    @Operation(summary = "Add product to cart")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @Valid @RequestBody AddCartItemRequest request
    ) {
        CartResponse cart = cartService.addItem(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Add item to cart successfully",
                        cart
                )
        );
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        CartResponse cart = cartService.updateItem(productId, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Update cart item successfully",
                        cart
                )
        );
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove product from cart")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @PathVariable UUID productId
    ) {
        cartService.removeItem(productId);

        return ResponseEntity.ok(
                ApiResponse.success("Remove cart item successfully")
        );
    }

    @DeleteMapping
    @Operation(summary = "Clear current user's cart")
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        cartService.clearCart();

        return ResponseEntity.ok(
                ApiResponse.success("Clear cart successfully")
        );
    }
}
