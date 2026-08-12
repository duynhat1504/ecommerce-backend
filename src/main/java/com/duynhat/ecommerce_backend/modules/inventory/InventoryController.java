package com.duynhat.ecommerce_backend.modules.inventory;


import com.duynhat.ecommerce_backend.common.core.dto.ApiResponse;
import com.duynhat.ecommerce_backend.common.core.dto.PageResponse;
import com.duynhat.ecommerce_backend.modules.inventory.dto.response.InventoryTransactionResponse;
import com.duynhat.ecommerce_backend.modules.inventory.enums.InventoryTransactionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/inventory")
@Tag(
        name = "Inventory",
        description = "Admin inventory APIs"
)
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping("/products/{productId}/transactions")
    @Operation(summary = "Get product inventory history")
    public ResponseEntity<ApiResponse<PageResponse<InventoryTransactionResponse>>> getProductTransactions(
            @PathVariable UUID productId,
            @RequestParam(required = false) InventoryTransactionType type,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<InventoryTransactionResponse> transactions =
                inventoryService.getProductTransactions(
                        productId,
                        type,
                        fromDate,
                        toDate,
                        page,
                        size
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get inventory transactions successfully",
                        PageResponse.from(transactions)
                )
        );
    }
}
