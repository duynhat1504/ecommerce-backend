package com.duynhat.ecommerce_backend.modules.product;

import com.duynhat.ecommerce_backend.common.core.dto.ApiResponse;
import com.duynhat.ecommerce_backend.common.core.dto.PageResponse;
import com.duynhat.ecommerce_backend.modules.product.dto.request.AdjustProductStockRequest;
import com.duynhat.ecommerce_backend.modules.product.dto.request.CreateProductRequest;
import com.duynhat.ecommerce_backend.modules.product.dto.request.ProductQueryRequest;
import com.duynhat.ecommerce_backend.modules.product.dto.request.UpdateProductRequest;
import com.duynhat.ecommerce_backend.modules.product.dto.response.ProductResponse;
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
@RequestMapping("/api/products")
@Tag(name = "Product", description = "Product management APIs")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Create product",
            description = "Create a new product. ADMIN role is required."
    )
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody CreateProductRequest req) {
        ProductResponse product = productService.create(req);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Create product successfully",
                                product
                        )
                );
    }

    @GetMapping
    @Operation(
            summary = "Get products",
            description = "Get products with pagination, sorting, filtering, and full-text search"
    )
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> findProducts(
            @ModelAttribute ProductQueryRequest req
    ) {
        Page<ProductResponse> products = productService.findProducts(req);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get products successfully",
                        PageResponse.from(products)
                )
        );
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get product by id",
            description = "Get product detail by product id"
    )
    public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable UUID id) {
        ProductResponse product = productService.getById(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get product successfully",
                        product
                )
        );
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Update product",
            description = "Update product by id. ADMIN role is required"
    )
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest req) {
        ProductResponse product = productService.update(id, req);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Update product successfully",
                        product
                )
        );
    }

    @PostMapping("/{id}/stock-adjustments")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Adjust product stock",
            description = "Increase or decrease product stock. ADMIN role is required"
    )
    public ResponseEntity<ApiResponse<ProductResponse>> adjustStock(
            @PathVariable UUID id,
            @Valid @RequestBody AdjustProductStockRequest req
    ) {
        ProductResponse product = productService.adjustStock(id, req);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Adjust product stock successfully",
                        product
                )
        );
    }
}
