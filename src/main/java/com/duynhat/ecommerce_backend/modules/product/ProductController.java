package com.duynhat.ecommerce_backend.modules.product;

import com.duynhat.ecommerce_backend.modules.product.dto.request.CreateProductRequest;
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
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(req));
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort
    ) {
        return ResponseEntity.ok(productService.getAll(page, size, sort));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> searchFullText(
            @RequestParam String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(productService.searchFullText(keyword, categoryId, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateProductRequest req) {
        return ResponseEntity.ok(productService.update(id, req));
    }
}
