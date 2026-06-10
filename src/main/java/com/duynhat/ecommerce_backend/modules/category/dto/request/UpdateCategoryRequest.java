package com.duynhat.ecommerce_backend.modules.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCategoryRequest {

    @NotBlank(message = "Category name is required")
    private String name;

    private String description;

    private Boolean active;
}
