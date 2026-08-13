package com.duynhat.ecommerce_backend.modules.category;

import com.duynhat.ecommerce_backend.modules.category.dto.request.CreateCategoryRequest;
import com.duynhat.ecommerce_backend.modules.category.dto.request.UpdateCategoryRequest;
import com.duynhat.ecommerce_backend.modules.category.dto.response.CategoryResponse;
import com.duynhat.ecommerce_backend.modules.category.entity.Category;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    CategoryResponse create(CreateCategoryRequest request);
    List<CategoryResponse> getAll();
    CategoryResponse getById(UUID id);
    CategoryResponse update(UUID id, UpdateCategoryRequest request);
    void delete(UUID id);
    Category findCategoryById(UUID id);
    Category findCategoryByNameIgnoreCase(String name);
    List<CategoryResponse> getAllForAdmin(Boolean active);
    CategoryResponse getByIdForAdmin(UUID id);
}
