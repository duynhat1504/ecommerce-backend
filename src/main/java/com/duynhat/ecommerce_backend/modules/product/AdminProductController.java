package com.duynhat.ecommerce_backend.modules.product;


import com.duynhat.ecommerce_backend.common.core.dto.ApiResponse;
import com.duynhat.ecommerce_backend.common.core.dto.PageResponse;
import com.duynhat.ecommerce_backend.modules.product.dto.request.AdminProductQueryRequest;
import com.duynhat.ecommerce_backend.modules.product.dto.response.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products")
@Tag(
        name = "Admin Product",
        description = "Admin product APIs"
)
@SecurityRequirement(name = "bearerAuth")
public class AdminProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/{id}")
    @Operation(
            summary = "Get product by id for admin",
            description = "Get product regardless of active status. ADMIN role is required"
    )
    public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable UUID id) {
        ProductResponse product = productService.getByIdForAdmin(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get product successfully",
                        product
                )
        );
    }

    @GetMapping
    @Operation(
            summary = "Get products for admin",
            description =
                    "Get all products including inactive products. ADMIN role is required"
    )
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> findProducts(
            @ModelAttribute AdminProductQueryRequest req
    ) {
        Page<ProductResponse> products =
                productService.findProductsForAdmin(req);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get products successfully",
                        PageResponse.from(products)
                )
        );
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete product",
            description = "Soft delete a product. ADMIN role is required"
    )
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        productService.delete(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Delete product successfully",
                        null
                )
        );
    }
}
