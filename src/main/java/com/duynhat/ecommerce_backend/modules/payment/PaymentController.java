package com.duynhat.ecommerce_backend.modules.payment;

import com.duynhat.ecommerce_backend.common.core.dto.ApiResponse;
import com.duynhat.ecommerce_backend.modules.payment.dto.request.CreatePaymentRequest;
import com.duynhat.ecommerce_backend.modules.payment.dto.response.PaymentResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        PaymentResponse payment = paymentService.createPayment(
                request,
                idempotencyKey
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Payment processed successfully",
                                payment
                        )
                );
    }
}
