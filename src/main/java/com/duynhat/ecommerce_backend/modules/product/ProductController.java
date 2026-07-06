package com.duynhat.ecommerce_backend.modules.product;

import com.duynhat.ecommerce_backend.common.core.dto.ApiResponse;
import com.duynhat.ecommerce_backend.common.core.dto.PageResponse;
import com.duynhat.ecommerce_backend.modules.product.dto.request.CreateProductRequest;
import com.duynhat.ecommerce_backend.modules.product.dto.request.ProductQueryRequest;
import com.duynhat.ecommerce_backend.modules.product.dto.request.UpdateProductRequest;
import com.duynhat.ecommerce_backend.modules.product.dto.response.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping
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
}
