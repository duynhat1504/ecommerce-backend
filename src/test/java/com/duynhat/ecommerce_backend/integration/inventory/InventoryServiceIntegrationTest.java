package com.duynhat.ecommerce_backend.integration.inventory;

import com.duynhat.ecommerce_backend.integration.AbstractIntegrationTest;
import com.duynhat.ecommerce_backend.modules.category.CategoryRepository;
import com.duynhat.ecommerce_backend.modules.category.entity.Category;
import com.duynhat.ecommerce_backend.modules.inventory.InventoryTransactionRepository;
import com.duynhat.ecommerce_backend.modules.inventory.InventoryService;
import com.duynhat.ecommerce_backend.modules.product.ProductRepository;
import com.duynhat.ecommerce_backend.modules.product.entity.Product;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @AfterEach
    void cleanUp() {
        inventoryTransactionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void getProductTransactions_withoutOptionalFilters_returnsEmptyPage() {
        Category category = new Category();
        category.setName("Inventory integration category");
        category.setActive(true);
        categoryRepository.save(category);

        Product product = new Product();
        product.setName("Inventory integration product");
        product.setPrice(BigDecimal.ONE);
        product.setStock(10);
        product.setActive(true);
        product.setCategory(category);
        productRepository.save(product);

        var result = inventoryService.getProductTransactions(
                product.getId(),
                null,
                null,
                null,
                0,
                10
        );

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }
}
