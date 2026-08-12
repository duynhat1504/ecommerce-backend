package com.duynhat.ecommerce_backend.modules.product;


import com.duynhat.ecommerce_backend.common.core.dto.ApiResponse;
import com.duynhat.ecommerce_backend.modules.product.dto.response.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
