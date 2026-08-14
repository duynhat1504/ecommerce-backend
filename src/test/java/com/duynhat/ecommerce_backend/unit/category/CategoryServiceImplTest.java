package com.duynhat.ecommerce_backend.unit.category;

import com.duynhat.ecommerce_backend.common.core.exception.ResourceNotFoundException;
import com.duynhat.ecommerce_backend.modules.category.CategoryRepository;
import com.duynhat.ecommerce_backend.modules.category.dto.request.CreateCategoryRequest;
import com.duynhat.ecommerce_backend.modules.category.dto.request.UpdateCategoryRequest;
import com.duynhat.ecommerce_backend.modules.category.entity.Category;
import com.duynhat.ecommerce_backend.modules.category.impl.CategoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void create_withUniqueName_shouldCreateActiveCategory() {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Laptop");
        request.setDescription("Portable computers");

        when(categoryRepository.existsCategoryByNameIgnoreCase("Laptop")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(UUID.randomUUID());
            return category;
        });

        var response = categoryService.create(request);

        assertThat(response.getName()).isEqualTo("Laptop");
        assertThat(response.getDescription()).isEqualTo("Portable computers");
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void create_withDuplicateName_shouldRejectWithoutSaving() {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Laptop");
        when(categoryRepository.existsCategoryByNameIgnoreCase("Laptop")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("Category name already exists");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_whenNameBelongsToAnotherCategory_shouldReject() {
        UUID categoryId = UUID.randomUUID();
        Category category = category(categoryId, "Laptop");
        Category existing = category(UUID.randomUUID(), "Gaming");
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName(" Gaming ");

        when(categoryRepository
                .findByIdAndDeletedAtIsNull(categoryId))
                .thenReturn(Optional.of(category));;

        when(categoryRepository.findCategoryByNameIgnoreCase("Gaming")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> categoryService.update(categoryId, request))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("Category name already exists");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void findCategoryById_whenMissing_shouldThrowNotFound() {
        UUID categoryId = UUID.randomUUID();

        when(categoryRepository
                .findByIdAndDeletedAtIsNull(categoryId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findCategoryById(categoryId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");
    }

    private Category category(UUID id, String name) {
        return Category.builder()
                .id(id)
                .name(name)
                .active(true)
                .build();
    }
}
