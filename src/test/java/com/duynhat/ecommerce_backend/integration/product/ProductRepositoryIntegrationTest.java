package com.duynhat.ecommerce_backend.integration.product;

import com.duynhat.ecommerce_backend.integration.AbstractIntegrationTest;
import com.duynhat.ecommerce_backend.modules.category.CategoryRepository;
import com.duynhat.ecommerce_backend.modules.category.entity.Category;
import com.duynhat.ecommerce_backend.modules.product.ProductRepository;
import com.duynhat.ecommerce_backend.modules.product.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ProductRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void searchFullTextWithFilters_whenKeywordMatches_shouldReturnProduct() {
        Category category = new Category();
        category.setName("Laptop");
        category.setDescription("Laptop products");
        category.setActive(true);

        category = categoryRepository.saveAndFlush(category);

        Product macbook = new Product();
        macbook.setName("MacBook Air M2");
        macbook.setDescription("Apple laptop with M2 processor and 512GB SSD");
        macbook.setPrice(new BigDecimal("1300.00"));
        macbook.setStock(10);
        macbook.setActive(true);
        macbook.setCategory(category);

        productRepository.saveAndFlush(macbook);

        Product mouse = new Product();
        mouse.setName("Gaming Mouse");
        mouse.setDescription("Wireless mouse");
        mouse.setPrice(new BigDecimal("50.00"));
        mouse.setStock(20);
        mouse.setActive(true);
        mouse.setCategory(category);

        productRepository.saveAndFlush(mouse);

        Page<Product> result =
                productRepository.searchFullTextWithFilters(
                        "macbook",
                        category.getId(),
                        null,
                        null,
                        PageRequest.of(0, 10)
                );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
                .extracting(Product::getName)
                .containsExactly("MacBook Air M2");
    }

    @Test
    void searchFullTextWithFilters_whenKeywordDoesNotMatch_shouldReturnEmpty() {
        Page<Product> result =
                productRepository.searchFullTextWithFilters(
                        "nonexistent-product",
                        null,
                        null,
                        null,
                        PageRequest.of(0, 10)
                );

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void searchFullTextWithFilters_whenOneMatchingProductIsDeleted_shouldExcludeItFromContentAndCount() {
        Category category = new Category();
        category.setName("Laptop");
        category.setDescription("Laptop products");
        category.setActive(true);
        category = categoryRepository.saveAndFlush(category);

        Product availableProduct = new Product();
        availableProduct.setName("MacBook Air");
        availableProduct.setDescription("MacBook laptop");
        availableProduct.setPrice(new BigDecimal("1200.00"));
        availableProduct.setStock(10);
        availableProduct.setActive(true);
        availableProduct.setCategory(category);
        productRepository.saveAndFlush(availableProduct);

        Product deletedProduct = new Product();
        deletedProduct.setName("MacBook Pro");
        deletedProduct.setDescription("MacBook professional laptop");
        deletedProduct.setPrice(new BigDecimal("2000.00"));
        deletedProduct.setStock(10);
        deletedProduct.setActive(true);
        deletedProduct.setCategory(category);
        deletedProduct.setDeletedAt(LocalDateTime.now());
        productRepository.saveAndFlush(deletedProduct);

        Page<Product> result = productRepository.searchFullTextWithFilters(
                "macbook",
                category.getId(),
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent())
                .extracting(Product::getId)
                .containsExactly(availableProduct.getId());

        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}
