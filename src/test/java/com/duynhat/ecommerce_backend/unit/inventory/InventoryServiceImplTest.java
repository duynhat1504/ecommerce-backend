package com.duynhat.ecommerce_backend.unit.inventory;

import com.duynhat.ecommerce_backend.modules.inventory.InventoryService;
import com.duynhat.ecommerce_backend.modules.inventory.InventoryTransactionRepository;
import com.duynhat.ecommerce_backend.modules.inventory.impl.InventoryServiceImpl;
import com.duynhat.ecommerce_backend.modules.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Test
    void getProductTransactions_withoutOptionalFilters_usesNonNullSpecification() {
        UUID productId = UUID.randomUUID();

        when(productRepository.existsById(productId)).thenReturn(true);
        when(inventoryTransactionRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        var result = inventoryService.getProductTransactions(
                productId,
                null,
                null,
                null,
                0,
                10
        );

        ArgumentCaptor<Specification> specificationCaptor =
                ArgumentCaptor.forClass(Specification.class);

        verify(inventoryTransactionRepository).findAll(
                specificationCaptor.capture(),
                any(Pageable.class)
        );

        assertThat(specificationCaptor.getValue()).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }
}
