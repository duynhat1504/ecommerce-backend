package com.duynhat.ecommerce_backend.modules.product.impl;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.common.core.exception.ResourceNotFoundException;
import com.duynhat.ecommerce_backend.modules.category.CategoryService;
import com.duynhat.ecommerce_backend.modules.category.entity.Category;
import com.duynhat.ecommerce_backend.modules.inventory.InventoryTransactionRepository;
import com.duynhat.ecommerce_backend.modules.inventory.entity.InventoryTransaction;
import com.duynhat.ecommerce_backend.modules.inventory.enums.InventoryTransactionType;
import com.duynhat.ecommerce_backend.modules.product.ProductRepository;
import com.duynhat.ecommerce_backend.modules.product.ProductService;
import com.duynhat.ecommerce_backend.modules.product.dto.request.*;
import com.duynhat.ecommerce_backend.modules.product.dto.response.ProductResponse;
import com.duynhat.ecommerce_backend.modules.product.entity.Product;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name",
            "price",
            "stock",
            "createdAt",
            "updatedAt"
    );

    @Override
    public ProductResponse create(CreateProductRequest req) {
        Category category = getActiveCategory(req.getCategoryName());

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
            throw e;
        }
    }

    @Override
    public Page<ProductResponse> findProducts(ProductQueryRequest req) {
        int page = req.getPage() == null ? 0 : req.getPage();
        int size = req.getSize() == null ? 10 : req.getSize();

        validatePagination(page, size);
        validatePriceRange(req.getMinPrice(), req.getMaxPrice());

        String keyword = normalizeKeyword(req.getKeyword());

        if (keyword != null) {
            Pageable pageable = PageRequest.of(page, size);

            if (req.getSort() == null || req.getSort().isBlank()) {
                return productRepository
                        .searchFullTextWithFilters(
                                keyword,
                                req.getCategoryId(),
                                req.getMinPrice(),
                                req.getMaxPrice(),
                                pageable
                        )
                        .map(this::toResponse);
            }

            Sort sort = buildSort(req.getSort());

            Sort.Order order = sort.iterator().next();

            return productRepository.searchFullTextWithFiltersAndSort(
                    keyword,
                    req.getCategoryId(),
                    req.getMinPrice(),
                    req.getMaxPrice(),
                    order.getProperty(),
                    order.getDirection().name().toLowerCase(),
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
                pageable
        ).map(this::toResponse);
    }

    @Override
    public ProductResponse getById(UUID id) {
        Product product = productRepository
                .findByIdAndActiveTrueAndCategory_ActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        return toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest req) {
        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Category category = getActiveCategory(req.getCategoryName());

        product.setName(req.getName());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setImageUrl(req.getImageUrl());
        product.setCategory(category);

        if (req.getActive() != null) {
            product.setActive(req.getActive());
        }

        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse adjustStock(UUID id, AdjustProductStockRequest req) {
        if (req.getQuantity() == 0) {
            throw new BadRequestException("Stock adjustment must not be zero");
        }

        User performedBy = getCurrentUser();

        Product product = productRepository
                .findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        int stockBefore = product.getStock();

        long stockAfterValue = (long) stockBefore + req.getQuantity();

        if (stockAfterValue < 0) {
            throw new BadRequestException("Stock cannot be negative");
        }

        if (stockAfterValue > Integer.MAX_VALUE) {
            throw new BadRequestException("Stock exceeds supported limit");
        }

        int stockAfter = (int) stockAfterValue;

        product.setStock(stockAfter);

        productRepository.save(product);

        InventoryTransaction transaction = new InventoryTransaction();

        transaction.setProduct(product);
        transaction.setType(InventoryTransactionType.ADMIN_ADJUSTMENT);
        transaction.setQuantityChange(req.getQuantity());
        transaction.setStockBefore(stockBefore);
        transaction.setStockAfter(stockAfter);
        transaction.setPerformedBy(performedBy);
        transaction.setReason(req.getReason().trim());

        inventoryTransactionRepository.save(transaction);

        return toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getByIdForAdmin(UUID id) {
        Product product = productRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        return toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> findProductsForAdmin(AdminProductQueryRequest req) {
        int page = req.getPage() == null ? 0 : req.getPage();

        int size = req.getSize() == null ? 10 : req.getSize();

        validatePagination(page, size);

        Sort sort = buildSort(req.getSort());

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        sort
                );

        Page<Product> products;

        if (req.getActive() == null) {
            products = productRepository.findAll(pageable);
        } else {
            products = productRepository.findByActive(
                            req.getActive(),
                            pageable
                    );
        }

        return products.map(this::toResponse);
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

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal()
        )) {
            throw new BadRequestException("User is not authenticated");
        }

        return userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Category getActiveCategory(String categoryName) {
        Category category = categoryService.findCategoryByNameIgnoreCase(categoryName.trim());

        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new BadRequestException("Category is inactive");
        }

        return category;
    }
}
