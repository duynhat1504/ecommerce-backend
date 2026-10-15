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
import com.duynhat.ecommerce_backend.modules.payment.dto.response.VnPayReturnResult;
import com.duynhat.ecommerce_backend.modules.payment.entity.Payment;
import com.duynhat.ecommerce_backend.modules.payment.enums.PaymentMethod;
import com.duynhat.ecommerce_backend.modules.payment.enums.PaymentStatus;
import com.duynhat.ecommerce_backend.modules.payment.gateway.vnpay.VnPayPaymentUrlBuilder;
import com.duynhat.ecommerce_backend.modules.payment.gateway.vnpay.VnPayReturnVerifier;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {


    private final PaymentRepository paymentRepository;

    private final OrderRepository orderRepository;

    private final UserRepository userRepository;

    private final VnPayPaymentUrlBuilder vnPayPaymentUrlBuilder;

    private final VnPayReturnVerifier vnPayReturnVerifier;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            VnPayPaymentUrlBuilder vnPayPaymentUrlBuilder,
            VnPayReturnVerifier vnPayReturnVerifier
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.vnPayPaymentUrlBuilder = vnPayPaymentUrlBuilder;
        this.vnPayReturnVerifier = vnPayReturnVerifier;
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

    @Override
    @Transactional
    public VnPayReturnResult processVnPayReturn(
            Map<String, String> params
    ) {
        if (!vnPayReturnVerifier.isValid(params)) {
            throw new BadRequestException("Invalid VNPay signature");
        }

        String merchantTxnRef = params.get("vnp_TxnRef");

        if (merchantTxnRef == null || merchantTxnRef.isBlank()) {
            throw new BadRequestException("Missing VNPay transaction reference");
        }

        Payment payment = paymentRepository
                .findByMerchantTxnRefForUpdate(merchantTxnRef)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Payment not found"
                        )
                );

        validateVnPayAmount(
                payment,
                params.get("vnp_Amount")
        );

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return toVnPayReturnResult(payment);
        }

        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus =
                params.get("vnp_TransactionStatus");

        payment.setGatewayResponseCode(responseCode);

        String gatewayTransactionNo =
                params.get("vnp_TransactionNo");

        if (gatewayTransactionNo != null
                && !gatewayTransactionNo.isBlank()) {
            payment.setGatewayTransactionNo(
                    gatewayTransactionNo
            );
        }

        boolean successful =
                "00".equals(responseCode)
                        && "00".equals(transactionStatus);

        if (successful) {
            payment.setStatus(PaymentStatus.SUCCESS);

            Order order = payment.getOrder();

            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.CONFIRMED);
            }
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(
                    "VNPay payment failed with response code "
                            + responseCode
            );
        }

        Payment saved =
                paymentRepository.save(payment);

        return toVnPayReturnResult(saved);
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

    private void validateVnPayAmount(
            Payment payment,
            String vnPayAmount
    ) {
        if (vnPayAmount == null
                || vnPayAmount.isBlank()) {
            throw new BadRequestException(
                    "Missing VNPay amount"
            );
        }

        String expectedAmount;

        try {
            expectedAmount = payment
                    .getAmount()
                    .movePointRight(2)
                    .toBigIntegerExact()
                    .toString();
        } catch (ArithmeticException ex) {
            throw new BadRequestException(
                    "Invalid payment amount"
            );
        }

        if (!expectedAmount.equals(vnPayAmount)) {
            throw new BadRequestException(
                    "VNPay amount does not match payment"
            );
        }
    }

    private VnPayReturnResult toVnPayReturnResult(
            Payment payment
    ) {
        return VnPayReturnResult
                .builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .status(payment.getStatus())
                .success(
                        payment.getStatus()
                                == PaymentStatus.SUCCESS
                )
                .build();
    }
}
