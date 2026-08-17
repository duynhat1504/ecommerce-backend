package com.duynhat.ecommerce_backend.modules.order.impl;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.common.core.exception.ResourceNotFoundException;
import com.duynhat.ecommerce_backend.modules.cart.CartItemRepository;
import com.duynhat.ecommerce_backend.modules.cart.CartRepository;
import com.duynhat.ecommerce_backend.modules.cart.entity.Cart;
import com.duynhat.ecommerce_backend.modules.cart.entity.CartItem;
import com.duynhat.ecommerce_backend.modules.inventory.InventoryTransactionRepository;
import com.duynhat.ecommerce_backend.modules.inventory.entity.InventoryTransaction;
import com.duynhat.ecommerce_backend.modules.inventory.enums.InventoryTransactionType;
import com.duynhat.ecommerce_backend.modules.order.OrderRepository;
import com.duynhat.ecommerce_backend.modules.order.OrderService;
import com.duynhat.ecommerce_backend.modules.order.dto.request.CreateOrderRequest;
import com.duynhat.ecommerce_backend.modules.order.dto.request.UpdateOrderStatusRequest;
import com.duynhat.ecommerce_backend.modules.order.dto.response.OrderItemResponse;
import com.duynhat.ecommerce_backend.modules.order.dto.response.OrderResponse;
import com.duynhat.ecommerce_backend.modules.order.dto.response.OrderSummaryResponse;
import com.duynhat.ecommerce_backend.modules.order.entity.Order;
import com.duynhat.ecommerce_backend.modules.order.entity.OrderItem;
import com.duynhat.ecommerce_backend.modules.order.enums.OrderStatus;
import com.duynhat.ecommerce_backend.modules.product.ProductRepository;
import com.duynhat.ecommerce_backend.modules.product.entity.Product;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        User user = getCurrentUser();

        Cart cart = cartRepository
                .findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        List<CartItem> cartItems = cartItemRepository.findAllByCartId(cart.getId());

        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        List<UUID> productIds = cartItems.stream()
                .map(item -> item.getProduct().getId())
                .distinct()
                .sorted()
                .toList();

        List<Product> lockedProducts = productRepository.findOrderableByIdsForUpdate(productIds);

        if (lockedProducts.size() != productIds.size()) {
            throw new BadRequestException("One or more products no longer exist");
        }

        Map<UUID, Product> productMap = lockedProducts.stream()
                .collect(
                        Collectors.toMap(
                                Product::getId,
                                Function.identity()
                        )
                );

        Order order = new Order();
        order.setOrderCode(generateOrderCode());
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setRecipientName(request.getRecipientName().trim());
        order.setPhoneNumber(request.getPhoneNumber().trim());
        order.setShippingAddress(request.getShippingAddress().trim());

        BigDecimal totalAmount = BigDecimal.ZERO;

        List<InventoryTransaction> inventoryTransactions = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            UUID productId = cartItem.getProduct().getId();

            Product product = productMap.get(productId);

            if (product == null) {
                throw new ResourceNotFoundException("Product not found: " + productId);
            }

            int quantity = cartItem.getQuantity();

            validateProduct(product, quantity);

            BigDecimal unitPrice = product.getPrice();

            BigDecimal subtotal = unitPrice.multiply(
                    BigDecimal.valueOf(quantity)
            );

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setQuantity(quantity);
            orderItem.setSubtotal(subtotal);

            order.addItem(orderItem);

            totalAmount = totalAmount.add(subtotal);

            int stockBefore = product.getStock();

            int stockAfter = stockBefore - quantity;

            product.setStock(stockAfter);

            InventoryTransaction transaction = new InventoryTransaction();

            transaction.setProduct(product);
            transaction.setType(InventoryTransactionType.ORDER_CREATED);
            transaction.setQuantityChange(-quantity);
            transaction.setStockBefore(stockBefore);
            transaction.setStockAfter(stockAfter);
            transaction.setReason("Order created: " + order.getOrderCode());

            inventoryTransactions.add(transaction);
        }

        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        inventoryTransactions.forEach(transaction -> transaction.setOrder(savedOrder));

        inventoryTransactionRepository.saveAll(inventoryTransactions);

        int deletedItems = cartItemRepository.deleteAllByCartId(cart.getId());

        if (deletedItems != cartItems.size()) {
            throw new IllegalStateException("Unable to clear all cart items");
        }

        return toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrders(
            int page,
            int size
    ) {
        validatePagination(page, size);

        User user = getCurrentUser();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return orderRepository
                .findByUserId(user.getId(), pageable)
                .map(this::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getAllOrders(
            OrderStatus status,
            String orderCode,
            int page,
            int size
    ) {
        validatePagination(page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        String normalizedOrderCode =
                orderCode == null || orderCode.isBlank() ? null : orderCode.trim();

        Page<Order> orders;

        if (status == null && normalizedOrderCode == null) {
            orders = orderRepository.findAll(pageable);
        } else if (status != null && normalizedOrderCode == null) {
            orders = orderRepository.findByStatus(status, pageable);
        } else if (status == null) {
            orders = orderRepository.findByOrderCodeContainingIgnoreCase(
                    normalizedOrderCode,
                    pageable
            );
        } else {
            orders = orderRepository.findByStatusAndOrderCodeContainingIgnoreCase(
                    status,
                    normalizedOrderCode,
                    pageable
            );
        }

        return orders.map(this::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrderById(UUID orderId) {
        User user = getCurrentUser();

        Order order = orderRepository
                .findDetailByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest req) {
        Order order = orderRepository
                .findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus = req.getStatus();

        if (currentStatus == newStatus) {
            return toResponse(order);
        }

        validateStatusTransition(currentStatus, newStatus);

        if (newStatus == OrderStatus.CANCELLED) {
            restoreProductStock(
                    order,
                    InventoryTransactionType.ORDER_CANCELLED,
                    "Order cancelled: " + order.getOrderCode()
            );
        }

        order.setStatus(newStatus);

        return toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse cancelMyOrder(UUID orderId) {
        User user = getCurrentUser();

        Order order = orderRepository.findByIdAndUserIdForUpdate(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return toResponse(order);
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Only pending orders can be cancelled");
        }

        restoreProductStock(
                order,
                InventoryTransactionType.ORDER_CANCELLED,
                "Order cancelled: " + order.getOrderCode()
        );

        order.setStatus(OrderStatus.CANCELLED);

        return toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        return toResponse(order);
    }

    @Override
    @Transactional
    public boolean expireOrderIfEligible(
            UUID orderId,
            LocalDateTime cutoff
    ) {

        Order order = orderRepository
                .findByIdForUpdate(orderId)
                .orElse(null);

        if (order == null) {
            return false;
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            return false;
        }

        if (order.getCreatedAt() == null
                || !order.getCreatedAt().isBefore(cutoff)) {
            return false;
        }

        restoreProductStock(
                order,
                InventoryTransactionType.ORDER_EXPIRED,
                "Order expired: "
                        + order.getOrderCode()
        );

        order.setStatus(OrderStatus.EXPIRED);

        return true;
    }

    private void validateProduct(Product product, int quantity) {
        if (product.getDeletedAt() != null) {
            throw new BadRequestException("Product is no longer available" + product.getName());
        }

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new BadRequestException("Product is not available: " + product.getName());
        }

        if (product.getCategory().getDeletedAt() != null) {
            throw new BadRequestException("Product category is not available" + product.getName());
        }

        if (!Boolean.TRUE.equals(product.getCategory().getActive())) {
            throw new BadRequestException("Product category is not available: " + product.getName());
        }

        if (product.getStock() == null || product.getStock() < quantity) {
            throw new BadRequestException("Insufficient stock for product: " + product.getName());
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BadRequestException("User is not authenticated");
        }

        return userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String generateOrderCode() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        String randomPart = UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();

        return "ORD-" + timestamp + "-" + randomPart;
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page index must not be negative");
        }

        if (size <= 0 || size > 100) {
            throw new BadRequestException("Page size must be between 1 and 100");
        }
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        boolean valid = switch (currentStatus) {
            case PENDING ->
                newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED;
            case CONFIRMED ->
                newStatus == OrderStatus.SHIPPING || newStatus == OrderStatus.CANCELLED;
            case SHIPPING ->
                newStatus == OrderStatus.COMPLETED;
            case COMPLETED, CANCELLED, EXPIRED -> false;
        };

        if (!valid) {
            throw new BadRequestException(
                    "Cannot change order status from "
                            + currentStatus
                            + " to "
                            + newStatus
            );
        }
    }

    private OrderSummaryResponse toSummaryResponse(Order order) {
        int totalItems = order.getItems()
                .stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        return OrderSummaryResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .totalItems(totalItems)
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .recipientName(order.getRecipientName())
                .phoneNumber(order.getPhoneNumber())
                .shippingAddress(order.getShippingAddress())
                .items(
                        order.getItems()
                                .stream()
                                .map(this::toItemResponse)
                                .toList()
                )
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        UUID productId = item.getProduct() == null
                ? null
                : item.getProduct().getId();

        return OrderItemResponse.builder()
                .productId(productId)
                .productName(item.getProductName())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }

    protected void restoreProductStock(
            Order order,
            InventoryTransactionType transactionType,
            String reason
    ) {
        Map<UUID, Integer> quantityByProductId = order.getItems()
                .stream()
                .filter(item -> item.getProduct() != null)
                .collect(
                        Collectors.groupingBy(
                                item -> item.getProduct().getId(),
                                Collectors.summingInt(OrderItem::getQuantity)
                        )
                );

        if (quantityByProductId.isEmpty()) {
            return;
        }

        List<UUID> productIds = quantityByProductId
                .keySet()
                .stream()
                .sorted()
                .toList();

        List<Product> lockedProducts = productRepository.findAllByIdForUpdate(productIds);

        if (lockedProducts.size() != productIds.size()) {
            throw new ResourceNotFoundException("One or more products no longer available");
        }

        Map<UUID, Product> productMap = lockedProducts
                .stream()
                .collect(
                        Collectors.toMap(
                                Product::getId,
                                Function.identity()
                        )
                );

        List<InventoryTransaction> inventoryTransactions = new ArrayList<>();

        for (UUID productId : productIds) {
            Product product = productMap.get(productId);

            if (product == null) {
                throw new ResourceNotFoundException("Product not found: " + productId);
            }

            int quantity = quantityByProductId.get(productId);

            int stockBefore = product.getStock();

            long stockAfterValue = (long) stockBefore + quantity;

            if (stockAfterValue > Integer.MAX_VALUE) {
                throw new BadRequestException("Stock exceeds supported limit");
            }

            int stockAfter = (int) stockAfterValue;

            product.setStock(stockAfter);

            InventoryTransaction transaction = new InventoryTransaction();

            transaction.setProduct(product);
            transaction.setOrder(order);
            transaction.setType(transactionType);
            transaction.setQuantityChange(quantity);
            transaction.setStockBefore(stockBefore);
            transaction.setStockAfter(stockAfter);
            transaction.setReason(reason);

            inventoryTransactions.add(transaction);
        }

        inventoryTransactionRepository.saveAll(inventoryTransactions);
    }
}
