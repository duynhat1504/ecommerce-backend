package com.duynhat.ecommerce_backend.modules.payment.dto.response;

import com.duynhat.ecommerce_backend.modules.payment.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class VnPayReturnResult {

    private UUID paymentId;

    private UUID orderId;

    private PaymentStatus status;

    private boolean success;
}