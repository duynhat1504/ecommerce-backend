package com.duynhat.ecommerce_backend.modules.order;

import com.duynhat.ecommerce_backend.common.core.dto.ApiResponse;
import com.duynhat.ecommerce_backend.common.core.dto.PageResponse;
import com.duynhat.ecommerce_backend.modules.order.dto.request.UpdateOrderStatusRequest;
import com.duynhat.ecommerce_backend.modules.order.dto.response.OrderResponse;
import com.duynhat.ecommerce_backend.modules.order.dto.response.OrderSummaryResponse;
import com.duynhat.ecommerce_backend.modules.order.enums.OrderStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
@Tag(name = "Admin Order", description = "Admin order management APIs")
@SecurityRequirement(name = "bearerAuth")
public class AdminOrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping
    @Operation(summary = "Get all orders")
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<OrderSummaryResponse> orders = orderService.getAllOrders(status, page, size);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get all orders successfully",
                        PageResponse.from(orders)
                )
        );
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update order status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest req
    ) {
        OrderResponse order = orderService.updateOrderStatus(id, req);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Update order status successfully",
                        order
                )
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order detail")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable UUID id) {
        OrderResponse order = orderService.getOrderById(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get order successfully",
                        order
                )
        );
    }
}
