package com.duynhat.ecommerce_backend.integration.category;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.common.core.exception.ResourceNotFoundException;
import com.duynhat.ecommerce_backend.integration.AbstractIntegrationTest;
import com.duynhat.ecommerce_backend.modules.category.CategoryRepository;
import com.duynhat.ecommerce_backend.modules.category.CategoryService;
import com.duynhat.ecommerce_backend.modules.category.entity.Category;
import com.duynhat.ecommerce_backend.modules.product.ProductRepository;
import com.duynhat.ecommerce_backend.modules.product.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
public class CategorySoftDeleteIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void deleteCategory_withoutProducts_shouldSoftDelete() {
        Category category = createCategory("Empty Category");

        categoryService.delete(category.getId());

        Category persisted = categoryRepository
                .findById(category.getId())
                .orElseThrow();

        assertThat(persisted.getDeletedAt()).isNotNull();

        assertThatThrownBy(() -> categoryService
                .getById(category.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");

        assertThat(categoryService
                .getByIdForAdmin(category.getId()).getDeletedAt())
                .isNotNull();
    }

    @Test
    void deleteCategory_withExistingProduct_shouldReject() {
        Category category = createCategory("Category With Product");

        Product product = Product.builder()
                .name("Product")
                .description("Product description")
                .price(new BigDecimal("100.00"))
                .stock(10)
                .active(true)
                .category(category)
                .build();

        productRepository.saveAndFlush(product);

        assertThatThrownBy(() -> categoryService
                .delete(category.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot delete category containing products");

        Category persisted = categoryRepository
                .findById(category.getId())
                .orElseThrow();

        assertThat(persisted.getDeletedAt()).isNull();
    }

    private Category createCategory(String name) {
        return categoryRepository.saveAndFlush(
                Category.builder()
                        .name(name)
                        .description(name + " description")
                        .active(true)
                        .build()
        );
    }
}
