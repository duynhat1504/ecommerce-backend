package com.duynhat.ecommerce_backend.modules.payment;

import com.duynhat.ecommerce_backend.modules.payment.entity.Payment;
import com.duynhat.ecommerce_backend.modules.payment.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    List<Payment> findAllByOrderIdOrderByCreatedAtDesc(UUID orderId);
    boolean existsByOrderIdAndStatus(UUID orderId, PaymentStatus paymentStatus);
    Optional<Payment> findByMerchantTxnRef(String merchantTxnRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p
        FROM Payment p
        JOIN FETCH p.order
        WHERE p.merchantTxnRef = :merchantTxnRef
        """)
    Optional<Payment> findByMerchantTxnRefForUpdate(
            @Param("merchantTxnRef") String merchantTxnRef
    );
}
