package com.duynhat.ecommerce_backend.modules.product.impl;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.modules.category.CategoryService;
import com.duynhat.ecommerce_backend.modules.category.entity.Category;
import com.duynhat.ecommerce_backend.modules.product.ProductRepository;
import com.duynhat.ecommerce_backend.modules.product.ProductService;
import com.duynhat.ecommerce_backend.modules.product.dto.request.CreateProductRequest;
import com.duynhat.ecommerce_backend.modules.product.dto.request.ProductQueryRequest;
import com.duynhat.ecommerce_backend.modules.product.dto.request.UpdateProductRequest;
import com.duynhat.ecommerce_backend.modules.product.dto.response.ProductResponse;
import com.duynhat.ecommerce_backend.modules.product.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryService categoryService;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name",
            "price",
            "stock",
            "createdAt",
            "updateAt"
    );

    @Override
    public ProductResponse create(CreateProductRequest req) {
        Category category = categoryService.findCategoryByNameIgnoreCase(req.getCategoryName().trim());

        Product product = Product.builder()
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .stock(req.getStock())
                .imageUrl(req.getImageUrl())
                .category(category)
                .active(true)
                .build();

        try {
            Product saved = productRepository.save(product);

            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Invalid category name");
        }
    }

    @Override
    public Page<ProductResponse> findProducts(ProductQueryRequest req) {
        int page = req.getPage() == null ? 0 : req.getPage();
        int size = req.getSize() == null ? 10 : req.getSize();

        validatePagination(page, size);
        validatePriceRange(req.getMinPrice(), req.getMaxPrice());

        String keyword = normalizeKeyword(req.getKeyword());

        Boolean active = req.getActive();

        if (keyword != null) {
            Pageable pageable = PageRequest.of(page, size);

            return productRepository.searchFullTextWithFilters(
                    keyword,
                    req.getCategoryId(),
                    req.getMinPrice(),
                    req.getMaxPrice(),
                    active,
                    pageable
            ).map(this::toResponse);
        }

        Sort sortObject = buildSort(req.getSort());
        Pageable pageable = PageRequest.of(
                page,
                size,
                sortObject
        );

        return productRepository.filterProducts(
                req.getCategoryId(),
                req.getMinPrice(),
                req.getMaxPrice(),
                active,
                pageable
        ).map(this::toResponse);
    }

    @Override
    public ProductResponse getById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Product not found"));

        return toResponse(product);
    }

    @Override
    public ProductResponse update(UUID id, UpdateProductRequest req) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Product not found"));

        Category category = categoryService.findCategoryByNameIgnoreCase(req.getCategoryName().trim());

        product.setName(req.getName());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setStock(req.getStock());
        product.setImageUrl(req.getImageUrl());
        product.setCategory(category);

        if (req.getActive() != null) {
            product.setActive(req.getActive());
        }

        return toResponse(productRepository.save(product));
    }

    private Sort buildSort(String sortPram) {
        if (sortPram == null || sortPram.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String[] parts = sortPram.split(",");

        if (parts.length > 2) {
             throw new BadRequestException("Sort format must be field,direction");
        }

        String field = parts[0].trim();

        if (field.isBlank()) {
            throw new BadRequestException("Sort field must not be blank");
        }

        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            throw new BadRequestException("Invalid sort field " + field);
        }

        String direction = parts.length == 2 ? parts[1].trim() : "asc";

        Sort.Direction sortDirection;

        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Sort direction must be asc or desc");
        }

        return Sort.by(sortDirection, field);
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page index must not be negative");
        }

        if (size <= 0 || size > 100) {
            throw new BadRequestException("Page size must be between 1 and 100");
        }
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Min price must not be negative");
        }

        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Max price must not be negative");
        }

        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("Min price must not be greater than max price");
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim();
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .active(product.getActive())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .build();
    }
}
