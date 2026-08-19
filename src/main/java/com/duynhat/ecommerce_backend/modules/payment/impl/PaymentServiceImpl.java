package com.duynhat.ecommerce_backend.modules.payment.impl;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.common.core.exception.ResourceNotFoundException;
import com.duynhat.ecommerce_backend.modules.order.OrderRepository;
import com.duynhat.ecommerce_backend.modules.order.entity.Order;
import com.duynhat.ecommerce_backend.modules.order.enums.OrderStatus;
import com.duynhat.ecommerce_backend.modules.payment.PaymentRepository;
import com.duynhat.ecommerce_backend.modules.payment.PaymentService;
import com.duynhat.ecommerce_backend.modules.payment.dto.request.CreatePaymentRequest;
import com.duynhat.ecommerce_backend.modules.payment.dto.request.CreateVnPayPaymentRequest;
import com.duynhat.ecommerce_backend.modules.payment.dto.response.PaymentResponse;
import com.duynhat.ecommerce_backend.modules.payment.dto.response.VnPayPaymentResponse;
import com.duynhat.ecommerce_backend.modules.payment.entity.Payment;
import com.duynhat.ecommerce_backend.modules.payment.enums.PaymentMethod;
import com.duynhat.ecommerce_backend.modules.payment.enums.PaymentStatus;
import com.duynhat.ecommerce_backend.modules.payment.gateway.vnpay.VnPayPaymentUrlBuilder;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {


    private final PaymentRepository paymentRepository;

    private final OrderRepository orderRepository;

    private final UserRepository userRepository;

    private final VnPayPaymentUrlBuilder vnPayPaymentUrlBuilder;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            VnPayPaymentUrlBuilder vnPayPaymentUrlBuilder
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.vnPayPaymentUrlBuilder = vnPayPaymentUrlBuilder;
    }

    @Override
    @Transactional
    public PaymentResponse createPayment(
            CreatePaymentRequest req,
            String idempotencyKey
    ) {
        if (req.getMethod() != PaymentMethod.MOCK) {
            throw new BadRequestException("This endpoint only supports MOCK payment");
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key is required");
        }

        String normalizedKey = idempotencyKey.trim();

        User user = getCurrentUser();

        Order order = orderRepository
                .findByIdAndUserIdForUpdate(req.getOrderId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Optional<Payment> existingPayment = paymentRepository
                .findByIdempotencyKey(normalizedKey);

        if (existingPayment.isPresent()) {
            Payment existing = existingPayment.get();

            if (!existing.getOrder().getId().equals(order.getId())) {
                throw new BadRequestException("Idempotency-Key has already been used for another order");
            }

            return toResponse(existing);
        }

        if (paymentRepository.existsByOrderIdAndStatus(
                order.getId(),
                PaymentStatus.SUCCESS
        )) {
            throw new BadRequestException("Order has already been paid");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cancelled order cannot be paid");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Only pending orders can be paid");
        }

        Payment payment = new Payment();

        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setMethod(req.getMethod());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey(normalizedKey);

        if (Boolean.TRUE.equals(req.getSuccess())) {
            payment.setStatus(PaymentStatus.SUCCESS);

            payment.setTransactionCode(generateTransactionCode());

            order.setStatus(OrderStatus.CONFIRMED);
        } else {
            payment.setStatus(PaymentStatus.FAILED);

            payment.setFailureReason("Mock payment failed");
        }

        Payment saved = paymentRepository.save(payment);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public VnPayPaymentResponse createVnPayPayment(
            CreateVnPayPaymentRequest req,
            String idempotencyKey,
            String clientIp
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key is required");
        }

        String normalizedKey = idempotencyKey.trim();

        User user = getCurrentUser();

        Order order = orderRepository
                .findByIdAndUserIdForUpdate(req.getOrderId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Optional<Payment> existingPayment = paymentRepository
                .findByIdempotencyKey(normalizedKey);

        if (existingPayment.isPresent()) {

            Payment existing = existingPayment.get();

            if (!existing
                    .getOrder()
                    .getId()
                    .equals(order.getId())
            ) {
                throw new BadRequestException(
                        "Idempotency-Key has already been used for another order"
                );
            }

            if (existing.getMethod() != PaymentMethod.VNPAY) {
                throw new BadRequestException(
                        "Idempotency-Key has already been used for another payment method"
                );
            }

            String paymentUrl =
                    existing.getStatus()
                            == PaymentStatus.PENDING
                            ? vnPayPaymentUrlBuilder.build(
                            existing,
                            clientIp
                    )
                            : null;

            return toVnPayResponse(
                    existing,
                    paymentUrl
            );
        }

        if (paymentRepository
                .existsByOrderIdAndStatus(order.getId(), PaymentStatus.SUCCESS)
        ) {
            throw new BadRequestException("Order has already been paid");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cancelled order cannot be paid");
        }

        if (order.getStatus() == OrderStatus.EXPIRED) {
            throw new BadRequestException("Expired order cannot be paid");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Only pending orders can be paid");
        }

        Payment payment = new Payment();

        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setMethod(PaymentMethod.VNPAY);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey(normalizedKey);
        payment.setMerchantTxnRef(generateVnPayTxnRef());

        Payment saved = paymentRepository.saveAndFlush(payment);

        String paymentUrl = vnPayPaymentUrlBuilder.build(saved, clientIp);

        return toVnPayResponse(saved, paymentUrl);
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

    private String generateTransactionCode() {
        return "PAY-"
                + UUID.randomUUID()
                .toString()
                .substring(0, 12)
                .toUpperCase();
    }

    private String generateVnPayTxnRef() {
        return "VNP"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .toUpperCase();
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .transactionCode(payment.getTransactionCode())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private VnPayPaymentResponse toVnPayResponse(
            Payment payment,
            String paymentUrl
    ) {

        return VnPayPaymentResponse
                .builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .merchantTxnRef(payment.getMerchantTxnRef())
                .paymentUrl(paymentUrl)
                .build();
    }
}
