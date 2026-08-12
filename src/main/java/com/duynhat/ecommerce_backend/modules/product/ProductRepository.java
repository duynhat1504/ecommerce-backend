package com.duynhat.ecommerce_backend.modules.product;

import com.duynhat.ecommerce_backend.modules.product.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
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
                JOIN categories c
                    ON c.id = p.category_id
                WHERE (:categoryId IS NULL OR p.category_id = :categoryId)
                  AND (:minPrice IS NULL OR p.price >= :minPrice)
                  AND (:maxPrice IS NULL OR p.price <= :maxPrice)
                  AND p.active = true
                  AND c.active = true
                  AND p.search_vector @@ websearch_to_tsquery(
                        'simple',
                        :keyword
                  )
                ORDER BY
                    ts_rank_cd(
                        p.search_vector,
                        websearch_to_tsquery(
                            'simple',
                            :keyword
                        )
                    ) DESC,
                    p.created_at DESC
                """,
            countQuery = """
                SELECT COUNT(*)
                FROM products p
                JOIN categories c
                    ON c.id = p.category_id
                WHERE (:categoryId IS NULL OR p.category_id = :categoryId)
                  AND (:minPrice IS NULL OR p.price >= :minPrice)
                  AND (:maxPrice IS NULL OR p.price <= :maxPrice)
                  AND p.active = true
                  AND c.active = true
                  AND p.search_vector @@ websearch_to_tsquery(
                        'simple',
                        :keyword
                  )
                """,
            nativeQuery = true
    )
    Page<Product> searchFullTextWithFilters(
            @Param("keyword") String keyword,
            @Param("categoryId") UUID categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    @Query("""
            SELECT p
            FROM Product p
            WHERE (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND p.active = true
              AND p.category.active = true
            """)
    Page<Product> filterProducts(
            @Param("categoryId") UUID categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Product p
            WHERE p.id IN :productIds
            ORDER BY p.id ASC
            """)
    List<Product> findAllByIdForUpdate(
            @Param("productIds") List<UUID> productIds
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p
        FROM Product p
        WHERE p.id = :productId
        """)
    Optional<Product> findByIdForUpdate(
            @Param("productId") UUID productId
    );

    Optional<Product> findByIdAndActiveTrueAndCategory_ActiveTrue(UUID id);
    Page<Product> findByActive (Boolean active, Pageable pageable);
}
