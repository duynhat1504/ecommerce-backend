package com.duynhat.ecommerce_backend.modules.category;

import com.duynhat.ecommerce_backend.modules.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    boolean existsCategoryByNameIgnoreCase(String name);
    Optional<Category> findCategoryByNameIgnoreCase(String name);
}
