package com.duynhat.ecommerce_backend.modules.category;

import com.duynhat.ecommerce_backend.common.core.dto.ApiResponse;
import com.duynhat.ecommerce_backend.modules.category.dto.request.CreateCategoryRequest;
import com.duynhat.ecommerce_backend.modules.category.dto.request.UpdateCategoryRequest;
import com.duynhat.ecommerce_backend.modules.category.dto.response.CategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Category", description = "Category management APIs")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Create category",
            description = "Create a new category. ADMIN role is required"
    )
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CreateCategoryRequest req
    ) {
        CategoryResponse category = categoryService.create(req);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Create category successfully",
                                category
                        )
                );
    }

    @GetMapping
    @Operation(
            summary = "Get categories",
            description = "Get all categories"
    )
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll() {
        List<CategoryResponse> categories = categoryService.getAll();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get categories successfully",
                        categories
                )
        );
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get category by id",
            description = "Get category detail by product id"
    )
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(@PathVariable UUID id) {
        CategoryResponse category = categoryService.getById(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get category successfully",
                        category
                )
        );
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Update category",
            description = "Update category by id. ADMIN role is required."
    )
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest req
    ) {
        CategoryResponse category = categoryService.update(id, req);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Update category successfully",
                        category
                )
        );
    }
}

