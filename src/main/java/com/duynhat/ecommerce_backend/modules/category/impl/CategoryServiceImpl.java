package com.duynhat.ecommerce_backend.modules.category.impl;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.common.core.exception.ResourceNotFoundException;
import com.duynhat.ecommerce_backend.modules.category.CategoryRepository;
import com.duynhat.ecommerce_backend.modules.category.CategoryService;
import com.duynhat.ecommerce_backend.modules.category.dto.request.CreateCategoryRequest;
import com.duynhat.ecommerce_backend.modules.category.dto.request.UpdateCategoryRequest;
import com.duynhat.ecommerce_backend.modules.category.dto.response.CategoryResponse;
import com.duynhat.ecommerce_backend.modules.category.entity.Category;
import com.duynhat.ecommerce_backend.modules.product.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.duynhat.ecommerce_backend.config.RedisCacheConfig.CATEGORY_DETAIL;
import static com.duynhat.ecommerce_backend.config.RedisCacheConfig.CATEGORY_LIST;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    @Transactional
    @CacheEvict(
            cacheNames = CATEGORY_LIST,
            allEntries = true
    )
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
    @Cacheable(
            cacheNames = CATEGORY_LIST,
            key = "'all'"
    )
    public List<CategoryResponse> getAll() {
        return categoryRepository
                .findAllByActiveTrueAndDeletedAtIsNull()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Cacheable(
            cacheNames = CATEGORY_DETAIL,
            key = "#id"
    )
    public CategoryResponse getById(UUID id) {
        Category category = categoryRepository
                .findByIdAndActiveTrueAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        return toResponse(category);
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(
                            cacheNames = CATEGORY_LIST,
                            allEntries = true
                    ),
                    @CacheEvict(
                            cacheNames = CATEGORY_DETAIL,
                            key = "#id"
                    )
            }
    )
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
        return categoryRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    @Override
    public Category findCategoryByNameIgnoreCase(String name) {
        return categoryRepository
                .findCategoryByNameIgnoreCaseAndDeletedAtIsNull(name.trim())
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

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(
                            cacheNames = CATEGORY_LIST,
                            allEntries = true
                    ),
                    @CacheEvict(
                            cacheNames = CATEGORY_DETAIL,
                            key = "#id"
                    )
            }
    )
    public void delete(UUID id) {
        Category category = categoryRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (category.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Category not found");
        }

        if (productRepository.existsByCategory_IdAndDeletedAtIsNull(id)) {
            throw new BadRequestException("Cannot delete category containing products");
        }

        category.setDeletedAt(LocalDateTime.now());
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .active(category.getActive())
                .deletedAt(category.getDeletedAt())
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
