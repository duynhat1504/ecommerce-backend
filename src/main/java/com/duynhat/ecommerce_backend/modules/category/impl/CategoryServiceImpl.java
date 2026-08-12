package com.duynhat.ecommerce_backend.modules.category.impl;

import com.duynhat.ecommerce_backend.common.core.exception.ResourceNotFoundException;
import com.duynhat.ecommerce_backend.modules.category.CategoryRepository;
import com.duynhat.ecommerce_backend.modules.category.CategoryService;
import com.duynhat.ecommerce_backend.modules.category.dto.request.CreateCategoryRequest;
import com.duynhat.ecommerce_backend.modules.category.dto.request.UpdateCategoryRequest;
import com.duynhat.ecommerce_backend.modules.category.dto.response.CategoryResponse;
import com.duynhat.ecommerce_backend.modules.category.entity.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public CategoryResponse create(CreateCategoryRequest req) {
        String normalizedName = req.getName().trim();

        String normalizedDescription = normalizeNullableText(
                req.getDescription());

        if (categoryRepository.existsCategoryByNameIgnoreCase(normalizedName)) {
            throw new DataIntegrityViolationException("Category name already exists");
        }

        Category category = Category.builder()
                .name(normalizedName)
                .description(normalizedDescription)
                .active(true)
                .build();

        Category saved = categoryRepository.save(category);

        return toResponse(saved);
    }

    @Override
    public List<CategoryResponse> getAll() {
        return categoryRepository
                .findAllByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse getById(UUID id) {
        Category category = categoryRepository
                .findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        return toResponse(category);
    }

    @Override
    public CategoryResponse update(UUID id, UpdateCategoryRequest req) {
        Category category = findCategoryById(id);

        String normalizedName = req.getName().trim();

        String normalizedDescription = normalizeNullableText(req.getDescription());

        categoryRepository
                .findCategoryByNameIgnoreCase(normalizedName)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new DataIntegrityViolationException("Category name already exists");
                    }
                });

        category.setName(normalizedName);

        category.setDescription(normalizedDescription);

        if (req.getActive() != null) {
            category.setActive(req.getActive());
        }

        return toResponse(categoryRepository.save(category));
    }

    @Override
    public Category findCategoryById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    @Override
    public Category findCategoryByNameIgnoreCase(String name) {
        return categoryRepository
                .findCategoryByNameIgnoreCase(name.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    @Override
    public List<CategoryResponse> getAllForAdmin(Boolean active) {
        List<Category> categories;

        if (active == null) {
            categories = categoryRepository.findAll();
        } else {
            categories = categoryRepository.findAllByActive(active);
        }

        return categories
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse getByIdForAdmin(UUID id) {
        Category category = categoryRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        return toResponse(category);
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .active(category.getActive())
                .build();
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty() ? null : normalized;
    }
}
