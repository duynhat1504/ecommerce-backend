package com.duynhat.ecommerce_backend.modules.category;

import com.duynhat.ecommerce_backend.common.core.dto.ApiResponse;
import com.duynhat.ecommerce_backend.modules.category.dto.response.CategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/categories")
@Tag(
        name = "Admin Category",
        description = "Admin category APIs"
)
@SecurityRequirement(name = "bearerAuth")
public class AdminCategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    @Operation(
            summary = "Get categories for admin",
            description =
                    "Get all categories including inactive categories"
    )
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll(
            @RequestParam(required = false) Boolean active
    ) {
        List<CategoryResponse> categories = categoryService.getAllForAdmin(active);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get categories successfully",
                        categories
                )
        );
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get category by id for admin",
            description =
                    "Get category regardless of active status"
    )
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(@PathVariable UUID id) {
        CategoryResponse category = categoryService.getByIdForAdmin(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get category successfully",
                        category
                )
        );
    }
}
