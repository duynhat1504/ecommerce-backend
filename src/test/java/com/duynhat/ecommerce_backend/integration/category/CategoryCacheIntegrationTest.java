package com.duynhat.ecommerce_backend.integration.category;

import com.duynhat.ecommerce_backend.common.core.exception.ResourceNotFoundException;
import com.duynhat.ecommerce_backend.integration.AbstractIntegrationTest;
import com.duynhat.ecommerce_backend.modules.category.CategoryRepository;
import com.duynhat.ecommerce_backend.modules.category.CategoryService;
import com.duynhat.ecommerce_backend.modules.category.dto.request.CreateCategoryRequest;
import com.duynhat.ecommerce_backend.modules.category.dto.request.UpdateCategoryRequest;
import com.duynhat.ecommerce_backend.modules.category.dto.response.CategoryResponse;
import com.duynhat.ecommerce_backend.modules.category.entity.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static com.duynhat.ecommerce_backend.config.RedisCacheConfig.CATEGORY_DETAIL;
import static com.duynhat.ecommerce_backend.config.RedisCacheConfig.CATEGORY_LIST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryCacheIntegrationTest
        extends AbstractIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        clearCaches();

        jdbcTemplate.execute(
                """
                TRUNCATE TABLE categories CASCADE
                """
        );
    }

    @Test
    void getAll_secondCall_shouldUseCachedValue() {
        createCategory("Electronics", "Original description");

        List<CategoryResponse> first = categoryService.getAll();

        assertThat(first)
                .singleElement()
                .satisfies(category ->
                        assertThat(
                                category.getDescription()
                        ).isEqualTo("Original description")
                );


        jdbcTemplate.update(
                """
                UPDATE categories
                SET description = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE name = ?
                """,
                "Changed directly in DB",
                "Electronics"
        );

        Category dbCategory = categoryRepository
                .findCategoryByNameIgnoreCase("Electronics")
                .orElseThrow();

        assertThat(
                dbCategory.getDescription()
        ).isEqualTo("Changed directly in DB");

        List<CategoryResponse> second = categoryService.getAll();

        assertThat(second)
                .singleElement()
                .satisfies(category ->
                        assertThat(
                                category.getDescription()
                        ).isEqualTo("Original description")
                );
    }

    @Test
    void getById_secondCall_shouldUseCachedValue() {
        Category category = createCategory("Books", "Original description");

        CategoryResponse first = categoryService.getById(category.getId());

        assertThat(
                first.getDescription()
        ).isEqualTo("Original description");

        jdbcTemplate.update(
                """
                UPDATE categories
                SET description = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                "Changed directly in DB",
                category.getId()
        );

        Category dbCategory = categoryRepository
                .findById(category.getId())
                .orElseThrow();

        assertThat(
                dbCategory.getDescription()
        ).isEqualTo("Changed directly in DB");

        CategoryResponse second =
                categoryService.getById(
                        category.getId()
                );

        assertThat(
                second.getDescription()
        ).isEqualTo(
                "Original description"
        );
    }

    @Test
    void create_shouldEvictCategoryListCache() {

        createCategory(
                "Electronics",
                "Electronic products"
        );

        /*
         * Prime cache:
         *
         * categoryList = [Electronics]
         */
        List<CategoryResponse> before =
                categoryService.getAll();

        assertThat(before)
                .extracting(
                        CategoryResponse::getName
                )
                .containsExactly(
                        "Electronics"
                );

        /*
         * CREATE phải invalidate list cache.
         */
        CreateCategoryRequest request =
                new CreateCategoryRequest();

        request.setName(
                "Books"
        );

        request.setDescription(
                "Book products"
        );

        categoryService.create(
                request
        );

        /*
         * Nếu cache KHÔNG bị evict:
         *
         * vẫn chỉ có Electronics.
         *
         * Nếu evict đúng:
         *
         * MISS → DB → Electronics + Books.
         */
        List<CategoryResponse> after =
                categoryService.getAll();

        assertThat(after)
                .extracting(
                        CategoryResponse::getName
                )
                .containsExactlyInAnyOrder(
                        "Electronics",
                        "Books"
                );
    }

    @Test
    void update_shouldEvictListAndDetailCaches() {

        Category category =
                createCategory(
                        "Electronics",
                        "Old description"
                );

        /*
         * Prime cả list và detail.
         */
        categoryService.getAll();

        categoryService.getById(
                category.getId()
        );

        UpdateCategoryRequest request =
                new UpdateCategoryRequest();

        request.setName(
                "Electronics Updated"
        );

        request.setDescription(
                "New description"
        );

        request.setActive(true);

        /*
         * UPDATE phải evict:
         *
         * categoryList
         * categoryDetail::<id>
         */
        categoryService.update(
                category.getId(),
                request
        );

        CategoryResponse detail =
                categoryService.getById(
                        category.getId()
                );

        assertThat(
                detail.getName()
        ).isEqualTo(
                "Electronics Updated"
        );

        assertThat(
                detail.getDescription()
        ).isEqualTo(
                "New description"
        );

        List<CategoryResponse> list =
                categoryService.getAll();

        assertThat(list)
                .singleElement()
                .satisfies(item -> {
                    assertThat(
                            item.getName()
                    ).isEqualTo(
                            "Electronics Updated"
                    );

                    assertThat(
                            item.getDescription()
                    ).isEqualTo(
                            "New description"
                    );
                });
    }

    @Test
    void delete_shouldEvictListAndDetailCaches() {

        Category category =
                createCategory(
                        "Temporary",
                        "Temporary category"
                );

        // Prime cả 2 cache
        categoryService.getAll();

        categoryService.getById(
                category.getId()
        );

        // Soft delete
        categoryService.delete(
                category.getId()
        );

        /*
         * Nếu list cache KHÔNG bị xóa,
         * category cũ vẫn xuất hiện.
         */
        List<CategoryResponse> categories =
                categoryService.getAll();

        assertThat(categories)
                .extracting(
                        CategoryResponse::getId
                )
                .doesNotContain(
                        category.getId()
                );

        /*
         * Nếu detail cache KHÔNG bị xóa,
         * method này sẽ trả cached CategoryResponse.
         *
         * Nếu evict đúng:
         * Redis MISS
         * → query DB
         * → category đã deleted
         * → ResourceNotFoundException
         */
        assertThatThrownBy(
                () -> categoryService.getById(
                        category.getId()
                )
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                );
    }

    @Test
    void update_shouldRepopulateCacheWithNewValue() {

        Category category =
                createCategory(
                        "Books",
                        "Version 1"
                );

        /*
         * Cache Version 1.
         */
        CategoryResponse cached =
                categoryService.getById(
                        category.getId()
                );

        assertThat(
                cached.getDescription()
        ).isEqualTo(
                "Version 1"
        );

        UpdateCategoryRequest request =
                new UpdateCategoryRequest();

        request.setName(
                "Books"
        );

        request.setDescription(
                "Version 2"
        );

        request.setActive(true);

        /*
         * Evict Version 1.
         */
        categoryService.update(
                category.getId(),
                request
        );

        /*
         * MISS → DB Version 2 → cache Version 2.
         */
        CategoryResponse afterUpdate =
                categoryService.getById(
                        category.getId()
                );

        assertThat(
                afterUpdate.getDescription()
        ).isEqualTo(
                "Version 2"
        );

        /*
         * Sửa DB trực tiếp thành Version 3.
         *
         * Cache vẫn đang chứa Version 2.
         */
        jdbcTemplate.update(
                """
                UPDATE categories
                SET description = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                "Version 3",
                category.getId()
        );

        /*
         * Phải trả Version 2 từ Redis.
         */
        CategoryResponse cachedAgain =
                categoryService.getById(
                        category.getId()
                );

        assertThat(
                cachedAgain.getDescription()
        ).isEqualTo(
                "Version 2"
        );
    }

    private Category createCategory(
            String name,
            String description
    ) {

        return categoryRepository
                .saveAndFlush(
                        Category.builder()
                                .name(name)
                                .description(
                                        description
                                )
                                .active(true)
                                .build()
                );
    }

    private void clearCaches() {

        Cache listCache =
                cacheManager.getCache(
                        CATEGORY_LIST
                );

        if (listCache != null) {
            listCache.clear();
        }

        Cache detailCache =
                cacheManager.getCache(
                        CATEGORY_DETAIL
                );

        if (detailCache != null) {
            detailCache.clear();
        }
    }
}