package com.duynhat.ecommerce_backend.modules.payment;

import com.duynhat.ecommerce_backend.modules.payment.dto.request.CreatePaymentRequest;
import com.duynhat.ecommerce_backend.modules.payment.dto.response.PaymentResponse;

public interface PaymentService {

    PaymentResponse createPayment(
            CreatePaymentRequest request,
            String idempotencyKey
    );
}
