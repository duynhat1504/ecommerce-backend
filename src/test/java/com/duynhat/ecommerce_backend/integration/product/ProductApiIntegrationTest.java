package com.duynhat.ecommerce_backend.integration.product;

import com.duynhat.ecommerce_backend.integration.AbstractIntegrationTest;
import com.duynhat.ecommerce_backend.modules.category.CategoryRepository;
import com.duynhat.ecommerce_backend.modules.category.entity.Category;
import com.duynhat.ecommerce_backend.modules.product.ProductRepository;
import com.duynhat.ecommerce_backend.modules.product.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
class ProductApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_withValidRequest_shouldReturn201AndPersistProduct() throws Exception {
        createCategory("Laptop");

        mockMvc.perform(
                        post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "MacBook Air M2",
                                          "description": "Apple laptop",
                                          "price": 1299.99,
                                          "stock": 10,
                                          "categoryName": "Laptop"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Create product successfully"))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.name").value("MacBook Air M2"))
                .andExpect(jsonPath("$.data.categoryName").value("Laptop"))
                .andExpect(jsonPath("$.data.active").value(true));

        assertThat(productRepository.findAll())
                .singleElement()
                .satisfies(product -> {
                    assertThat(product.getName()).isEqualTo("MacBook Air M2");
                    assertThat(product.getPrice()).isEqualByComparingTo("1299.99");
                    assertThat(product.getCategory().getName()).isEqualTo("Laptop");
                });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_withUnknownCategory_shouldReturn404() throws Exception {
        mockMvc.perform(
                        post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "MacBook Air M2",
                                          "description": "Apple laptop",
                                          "price": 1299.99,
                                          "stock": 10,
                                          "categoryName": "Unknown category"
                                        }
                                        """)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Category not found"));

        assertThat(productRepository.count()).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_withNegativePrice_shouldReturn400() throws Exception {
        mockMvc.perform(
                        post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Invalid product",
                                          "price": -1,
                                          "stock": 10,
                                          "categoryName": "Laptop"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.price").value("Price must be greater than 0"));

        assertThat(productRepository.count()).isZero();
    }

    @Test
    void getProducts_withSecondPage_shouldReturnCorrectPageAndMetadata() throws Exception {
        Category category = createCategory("Laptop");
        createProduct("Product 1", "100.00", true, category);
        createProduct("Product 2", "200.00", true, category);
        createProduct("Product 3", "300.00", true, category);
        createProduct("Product 4", "400.00", true, category);
        createProduct("Product 5", "500.00", true, category);
        productRepository.flush();

        mockMvc.perform(
                        get("/api/products")
                                .param("page", "1")
                                .param("size", "2")
                                .param("sort", "name,asc")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].name").value("Product 3"))
                .andExpect(jsonPath("$.data.content[1].name").value("Product 4"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.first").value(false))
                .andExpect(jsonPath("$.data.last").value(false))
                .andExpect(jsonPath("$.data.numberOfElements").value(2));
    }

    @Test
    void getProducts_withFilters_shouldReturnOnlyMatchingProduct() throws Exception {
        Category laptops = createCategory("Laptop");
        Category accessories = createCategory("Accessories");

        createProduct("Laptop Pro", "1200.00", true, laptops);
        createProduct("Budget Laptop", "500.00", true, laptops);
        createProduct("Old Laptop", "1500.00", false, laptops);
        createProduct("Premium Mouse", "1300.00", true, accessories);
        productRepository.flush();

        mockMvc.perform(
                        get("/api/products")
                                .param("categoryId", laptops.getId().toString())
                                .param("minPrice", "1000.00")
                                .param("maxPrice", "1400.00")
                                .param("active", "true")
                                .param("sort", "name,asc")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Laptop Pro"))
                .andExpect(jsonPath("$.data.content[0].categoryId").value(laptops.getId().toString()))
                .andExpect(jsonPath("$.data.content[0].price").value(1200.00))
                .andExpect(jsonPath("$.data.content[0].active").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_shouldSoftDeleteHideFromPublicButRemainVisibleForAdmin() throws Exception {
        Category category = createCategory("Soft Delete Category");

        Product product = createProduct(
                "Soft Delete Product",
                "999.00",
                true,
                category
        );

        mockMvc.perform(delete("/api/admin/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Delete product successfully"));

        Product persistedProduct = productRepository
                .findById(product.getId())
                .orElseThrow();

        assertThat(persistedProduct.getDeletedAt()).isNotNull();

        mockMvc.perform(get("/api/products/{id}", product.getId()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/admin/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id")
                        .value(product.getId().toString()))
                .andExpect(jsonPath("$.data.deletedAt").isNotEmpty());
    }

    private Category createCategory(String name) {
        return categoryRepository.saveAndFlush(
                Category.builder()
                        .name(name)
                        .description(name + " products")
                        .active(true)
                        .build()
        );
    }

    private Product createProduct(
            String name,
            String price,
            boolean active,
            Category category
    ) {
        return productRepository.save(
                Product.builder()
                        .name(name)
                        .description(name + " description")
                        .price(new BigDecimal(price))
                        .stock(10)
                        .active(active)
                        .category(category)
                        .build()
        );
    }
}
