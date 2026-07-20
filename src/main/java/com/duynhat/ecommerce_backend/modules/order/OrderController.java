package com.duynhat.ecommerce_backend.modules.order;

import com.duynhat.ecommerce_backend.common.core.dto.ApiResponse;
import com.duynhat.ecommerce_backend.common.core.dto.PageResponse;
import com.duynhat.ecommerce_backend.modules.order.dto.request.CreateOrderRequest;
import com.duynhat.ecommerce_backend.modules.order.dto.response.OrderResponse;
import com.duynhat.ecommerce_backend.modules.order.dto.response.OrderSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order", description = "Customer order APIs")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    @Operation(summary = "Create order from current cart")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody CreateOrderRequest req) {
        OrderResponse order = orderService.createOrder(req);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Create order successfully",
                                order
                        )
                );
    }

    @GetMapping
    @Operation(summary = "Get current user's order")
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<OrderSummaryResponse> orders = orderService.getMyOrders(page, size);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get orders successfully",
                        PageResponse.from(orders)
                )
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get current user's order detail")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable UUID id) {
        OrderResponse order = orderService.getMyOrderById(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get order successfully",
                        order
                )
        );
    }
}
