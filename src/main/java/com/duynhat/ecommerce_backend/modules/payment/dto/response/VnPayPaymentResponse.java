package com.duynhat.ecommerce_backend.modules.payment.dto.response;

import com.duynhat.ecommerce_backend.modules.payment.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class VnPayPaymentResponse {

    private UUID paymentId;
    private UUID orderId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String merchantTxnRef;
    private String paymentUrl;
}
