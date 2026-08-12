package com.duynhat.ecommerce_backend.modules.inventory.impl;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.common.core.exception.ResourceNotFoundException;
import com.duynhat.ecommerce_backend.modules.inventory.InventoryService;
import com.duynhat.ecommerce_backend.modules.inventory.InventoryTransactionRepository;
import com.duynhat.ecommerce_backend.modules.inventory.dto.response.InventoryTransactionResponse;
import com.duynhat.ecommerce_backend.modules.inventory.entity.InventoryTransaction;
import com.duynhat.ecommerce_backend.modules.inventory.enums.InventoryTransactionType;
import com.duynhat.ecommerce_backend.modules.order.entity.Order;
import com.duynhat.ecommerce_backend.modules.product.ProductRepository;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryTransactionResponse> getProductTransactions(
            UUID productId,
            InventoryTransactionType type,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size
    ) {
        validatePagination(page, size);
        validateDateRange(fromDate, toDate);

        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException(
                    "Product not found"
            );
        }

        LocalDateTime fromDateTime = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime toDateExclusive = toDate == null ? null : toDate.plusDays(1).atStartOfDay();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );

        return inventoryTransactionRepository
                .findByProductWithFilters(
                        productId,
                        type,
                        fromDateTime,
                        toDateExclusive,
                        pageable
                )
                .map(this::toResponse);
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new BadRequestException(
                    "Page index must not be negative"
            );
        }

        if (size <= 0 || size > 100) {
            throw new BadRequestException(
                    "Page size must be between 1 and 100"
            );
        }
    }

    private InventoryTransactionResponse toResponse(InventoryTransaction transaction) {
        Order order = transaction.getOrder();

        User performedBy = transaction.getPerformedBy();

        return InventoryTransactionResponse.builder()
                .id(transaction.getId())
                .productId(transaction.getProduct().getId())
                .productName(transaction.getProduct().getName())
                .orderId(order == null ? null : order.getId())
                .orderCode(order == null ? null : order.getOrderCode())
                .performedById(performedBy == null ? null : performedBy.getId())
                .performedByEmail(performedBy == null ? null : performedBy.getEmail())
                .type(transaction.getType())
                .quantityChange(transaction.getQuantityChange())
                .stockBefore(transaction.getStockBefore())
                .stockAfter(transaction.getStockAfter())
                .reason(transaction.getReason())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null
                && toDate != null
                && fromDate.isAfter(toDate)) {
            throw new BadRequestException("From date must not be after to date");
        }
    }
}
