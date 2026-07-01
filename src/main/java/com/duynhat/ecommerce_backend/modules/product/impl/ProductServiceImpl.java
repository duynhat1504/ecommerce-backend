package com.duynhat.ecommerce_backend.modules.product.impl;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.modules.category.CategoryService;
import com.duynhat.ecommerce_backend.modules.category.entity.Category;
import com.duynhat.ecommerce_backend.modules.product.ProductRepository;
import com.duynhat.ecommerce_backend.modules.product.ProductService;
import com.duynhat.ecommerce_backend.modules.product.dto.request.CreateProductRequest;
import com.duynhat.ecommerce_backend.modules.product.dto.request.UpdateProductRequest;
import com.duynhat.ecommerce_backend.modules.product.dto.response.ProductResponse;
import com.duynhat.ecommerce_backend.modules.product.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryService categoryService;

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
    public Page<ProductResponse> getAll(int page, int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        Pageable pageable = PageRequest.of(page, size);

        Page<Product> products = productRepository.findAll(pageable);

        return products.map(this::toResponse);
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
