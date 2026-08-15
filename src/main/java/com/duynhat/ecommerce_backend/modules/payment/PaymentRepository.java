package com.duynhat.ecommerce_backend.modules.payment;

import com.duynhat.ecommerce_backend.modules.payment.entity.Payment;
import com.duynhat.ecommerce_backend.modules.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    List<Payment> findAllByOrderIdOrderByCreatedAtDesc(UUID orderId);
    boolean existsByOrderIdAndStatus(UUID orderId, PaymentStatus paymentStatus);
}
