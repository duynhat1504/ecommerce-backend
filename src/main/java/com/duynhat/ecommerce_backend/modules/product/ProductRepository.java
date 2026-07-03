package com.duynhat.ecommerce_backend.modules.product;

import com.duynhat.ecommerce_backend.modules.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query(
            value = """
                    SELECT
                        p.id,
                        p.name,
                        p.description,
                        p.price,
                        p.stock,
                        p.image_url,
                        p.active,
                        p.category_id,
                        p.created_at,
                        p.updated_at
                    FROM products p
                    WHERE p.active = true
                      AND p.search_vector @@ websearch_to_tsquery('simple', :keyword)
                    ORDER BY
                        ts_rank_cd(
                            p.search_vector, websearch_to_tsquery('simple', :keyword) 
                        ) DESC,
                        p.created_at DESC                                                            
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM products p
                    WHERE p.active = true
                      AND p.search_vector @@ websearch_to_tsquery('simple', :keyword)
                    """,
            nativeQuery = true
    )
    Page<Product> searchFullText(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT
                        p.id,
                        p.name,
                        p.description,
                        p.price,
                        p.stock,
                        p.image_url,
                        p.active,
                        p.category_id,
                        p.created_at,
                        p.updated_at
                    FROM products p
                    WHERE p.active = true
                      AND p.category_id = :categoryId
                      AND p.search_vector @@ websearch_to_tsquery('simple', :keyword)
                    ORDER BY
                        ts_rank_cd(
                            p.search_vector, websearch_to_tsquery('simple', :keyword)
                        ) DESC,
                        p.created_at DESC
                      
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM products p
                    WHERE p.active = true
                      AND p.category_id = :categoryId
                      AND p.search_vector @@ websearch_to_tsquery('simple', :keyword)
                    """,
            nativeQuery = true
    )
    Page<Product> searchFullTextByCategory(
            @Param("keyword") String keyword,
            @Param("categoryId") UUID categoryId,
            Pageable pageable
    );

    @Query("""
            SELECT p
            FROM Product p
            WHERE (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:active IS NULL OR p.active = :active)
            """)
    Page<Product> filterProducts(
            @Param("categoryId") UUID categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("active") Boolean active,
            Pageable pageable
    );
    boolean existsProductByNameIgnoreCase(String name);
    Optional<Product> findProductByNameIgnoreCase(String name);
}
