package com.duynhat.ecommerce_backend.integration;

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
                        true,
                        PageRequest.of(0, 10)
                );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
                .extracting(Product::getName)
                .containsExactly("Macbook Air M2");
    }

    @Test
    void searchFullTextWithFilters_whenKeywordDoesNotMatch_shouldReturnEmpty() {
        Page<Product> result =
                productRepository.searchFullTextWithFilters(
                        "nonexistent-product",
                        null,
                        null,
                        null,
                        true,
                        PageRequest.of(0, 10)
                );

        assertThat(result.getContent()).isEmpty();
    }
}
