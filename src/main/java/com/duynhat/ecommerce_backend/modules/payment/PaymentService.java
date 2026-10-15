package com.duynhat.ecommerce_backend.modules.payment;

import com.duynhat.ecommerce_backend.modules.payment.dto.request.CreatePaymentRequest;
import com.duynhat.ecommerce_backend.modules.payment.dto.request.CreateVnPayPaymentRequest;
import com.duynhat.ecommerce_backend.modules.payment.dto.response.PaymentResponse;
import com.duynhat.ecommerce_backend.modules.payment.dto.response.VnPayPaymentResponse;
import com.duynhat.ecommerce_backend.modules.payment.dto.response.VnPayReturnResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

public interface PaymentService {

    PaymentResponse createPayment(
            CreatePaymentRequest request,
            String idempotencyKey
    );
    VnPayPaymentResponse createVnPayPayment(
            CreateVnPayPaymentRequest request,
            String idempotencyKey,
            String clientIp
    );
    VnPayReturnResult processVnPayReturn(
            Map<String, String> params
    );
}
