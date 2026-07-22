package com.duynhat.ecommerce_backend.unit.product;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.modules.category.CategoryService;
import com.duynhat.ecommerce_backend.modules.product.ProductRepository;
import com.duynhat.ecommerce_backend.modules.product.dto.request.ProductQueryRequest;
import com.duynhat.ecommerce_backend.modules.product.entity.Product;
import com.duynhat.ecommerce_backend.modules.product.impl.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void findProducts_withKeyword_shouldUseFullTextSearchAndTrimKeyword() {
        ProductQueryRequest request = new ProductQueryRequest();
        request.setKeyword("  macbook air  ");
        request.setPage(2);
        request.setSize(5);

        when(productRepository.searchFullTextWithFilters(
                eq("macbook air"), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(Page.empty());

        productService.findProducts(request);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).searchFullTextWithFilters(
                eq("macbook air"), isNull(), isNull(), isNull(), isNull(), pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageableCaptor.getValue().getSort().isUnsorted()).isTrue();
        verify(productRepository, never()).filterProducts(any(), any(), any(), any(), any());
    }

    @Test
    void findProducts_withBlankKeyword_shouldUseFilterAndDefaultSort() {
        ProductQueryRequest request = new ProductQueryRequest();
        request.setKeyword("   ");
        request.setSort(null);

        when(productRepository.filterProducts(
                isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(Page.empty());

        productService.findProducts(request);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).filterProducts(
                isNull(), isNull(), isNull(), isNull(), pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt"))
                .isNotNull()
                .extracting(order -> order.getDirection().name())
                .isEqualTo("DESC");
        verify(productRepository, never()).searchFullTextWithFilters(
                any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void findProducts_withNegativePage_shouldRejectBeforeCallingRepository() {
        ProductQueryRequest request = new ProductQueryRequest();
        request.setPage(-1);

        assertThatThrownBy(() -> productService.findProducts(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Page index must not be negative");

        verifyNoInteractions(productRepository);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    void findProducts_withInvalidSize_shouldRejectBeforeCallingRepository(int size) {
        ProductQueryRequest request = new ProductQueryRequest();
        request.setSize(size);

        assertThatThrownBy(() -> productService.findProducts(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Page size must be between 1 and 100");

        verifyNoInteractions(productRepository);
    }

    @Test
    void findProducts_withNegativeMinPrice_shouldReject() {
        ProductQueryRequest request = new ProductQueryRequest();
        request.setMinPrice(new BigDecimal("-0.01"));

        assertThatThrownBy(() -> productService.findProducts(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Min price must not be negative");
    }

    @Test
    void findProducts_withMinPriceGreaterThanMaxPrice_shouldReject() {
        ProductQueryRequest request = new ProductQueryRequest();
        request.setMinPrice(new BigDecimal("20"));
        request.setMaxPrice(new BigDecimal("10"));

        assertThatThrownBy(() -> productService.findProducts(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Min price must not be greater than max price");
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown,asc", "price,sideways", "price,asc,extra", ",asc"})
    void findProducts_withInvalidSort_shouldReject(String sort) {
        ProductQueryRequest request = new ProductQueryRequest();
        request.setSort(sort);

        assertThatThrownBy(() -> productService.findProducts(request))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(productRepository);
    }
}
