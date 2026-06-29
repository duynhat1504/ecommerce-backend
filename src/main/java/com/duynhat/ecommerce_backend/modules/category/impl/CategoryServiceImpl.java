package com.duynhat.ecommerce_backend.modules.category.impl;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
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
        if (categoryRepository.existsCategoryByNameIgnoreCase(req.getName())) {
            throw new BadRequestException("Category name already exists");
        }

        Category category = Category.builder()
                .name(req.getName())
                .description(req.getDescription())
                .active(true)
                .build();

        try {
            Category saved = categoryRepository.save(category);

            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Invalid category name");
        }
    }

    @Override
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse getById(UUID id) {
        Category category = findCategoryById(id);
        return toResponse(category);
    }

    @Override
    public CategoryResponse update(UUID id, UpdateCategoryRequest req) {
        Category category = findCategoryById(id);

        categoryRepository.findCategoryByNameIgnoreCase(req.getName().trim())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BadRequestException("Category name already exists");
                    }
                });

        category.setName(req.getName());
        category.setDescription(req.getDescription());

        if (req.getActive() != null) {
            category.setActive(req.getActive());
        }

        return toResponse(categoryRepository.save(category));
    }

    @Override
    public Category findCategoryById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Category not found"));
    }

    @Override
    public Category findCategoryByNameIgnoreCase(String name) {
        return categoryRepository.findCategoryByNameIgnoreCase(name.trim())
                .orElseThrow(() -> new BadRequestException("Category not found"));
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .active(category.getActive())
                .build();
    }
}
