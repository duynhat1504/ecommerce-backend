package com.duynhat.ecommerce_backend.modules.payment;

import com.duynhat.ecommerce_backend.common.core.dto.ApiResponse;
import com.duynhat.ecommerce_backend.modules.payment.dto.request.CreatePaymentRequest;
import com.duynhat.ecommerce_backend.modules.payment.dto.request.CreateVnPayPaymentRequest;
import com.duynhat.ecommerce_backend.modules.payment.dto.response.PaymentResponse;
import com.duynhat.ecommerce_backend.modules.payment.dto.response.VnPayPaymentResponse;
import com.duynhat.ecommerce_backend.modules.payment.dto.response.VnPayReturnResult;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Value("${app.frontend-url}")
    private String frontendUrl;

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

    @PostMapping("/vnpay")
    public ResponseEntity<ApiResponse<VnPayPaymentResponse>> createVnPayPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateVnPayPaymentRequest request,
            HttpServletRequest httpRequest
    ) {

        String clientIp = resolveClientIp(httpRequest);

        VnPayPaymentResponse payment = paymentService
                .createVnPayPayment(
                        request,
                        idempotencyKey,
                        clientIp
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "VNPay payment created successfully",
                                payment
                        )
                );
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<Void> handleVnPayReturn(
            @RequestParam Map<String, String> params
    ) {
        VnPayReturnResult result =
                paymentService.processVnPayReturn(params);

        String redirectUrl =
                frontendUrl
                        + "/payment-result"
                        + "?orderId="
                        + result.getOrderId()
                        + "&status="
                        + result.getStatus().name();

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
