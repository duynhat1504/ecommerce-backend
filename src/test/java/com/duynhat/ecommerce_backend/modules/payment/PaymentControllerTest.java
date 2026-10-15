package com.duynhat.ecommerce_backend.modules.payment;

import com.duynhat.ecommerce_backend.modules.payment.dto.response.VnPayReturnResult;
import com.duynhat.ecommerce_backend.modules.payment.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        paymentController =
                new PaymentController(paymentService);

        ReflectionTestUtils.setField(
                paymentController,
                "frontendUrl",
                "https://coffee.duynhat.dev"
        );
    }

    @Test
    void handleVnPayReturn_success_shouldRedirectToFrontend() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "VNP123456");
        params.put("vnp_Amount", "15000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_SecureHash", "valid-signature");

        VnPayReturnResult result =
                VnPayReturnResult.builder()
                        .paymentId(paymentId)
                        .orderId(orderId)
                        .status(PaymentStatus.SUCCESS)
                        .success(true)
                        .build();

        when(paymentService.processVnPayReturn(params))
                .thenReturn(result);

        ResponseEntity<Void> response =
                paymentController.handleVnPayReturn(params);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.FOUND);

        assertThat(response.getHeaders().getLocation())
                .isEqualTo(
                        URI.create(
                                "https://coffee.duynhat.dev"
                                        + "/payment-result"
                                        + "?orderId="
                                        + orderId
                                        + "&status=SUCCESS"
                        )
                );

        verify(paymentService)
                .processVnPayReturn(params);
    }

    @Test
    void handleVnPayReturn_failed_shouldRedirectToFrontend() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "VNPFAILED123");
        params.put("vnp_Amount", "20000000");
        params.put("vnp_ResponseCode", "24");
        params.put("vnp_TransactionStatus", "02");
        params.put("vnp_SecureHash", "valid-signature");

        VnPayReturnResult result =
                VnPayReturnResult.builder()
                        .paymentId(paymentId)
                        .orderId(orderId)
                        .status(PaymentStatus.FAILED)
                        .success(false)
                        .build();

        when(paymentService.processVnPayReturn(params))
                .thenReturn(result);

        ResponseEntity<Void> response =
                paymentController.handleVnPayReturn(params);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.FOUND);

        assertThat(response.getHeaders().getLocation())
                .isEqualTo(
                        URI.create(
                                "https://coffee.duynhat.dev"
                                        + "/payment-result"
                                        + "?orderId="
                                        + orderId
                                        + "&status=FAILED"
                        )
                );

        verify(paymentService)
                .processVnPayReturn(params);
    }
}