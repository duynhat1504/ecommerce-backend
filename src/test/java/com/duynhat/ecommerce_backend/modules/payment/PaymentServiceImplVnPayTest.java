package com.duynhat.ecommerce_backend.modules.payment;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.modules.order.OrderRepository;
import com.duynhat.ecommerce_backend.modules.order.entity.Order;
import com.duynhat.ecommerce_backend.modules.order.enums.OrderStatus;
import com.duynhat.ecommerce_backend.modules.payment.dto.response.VnPayReturnResult;
import com.duynhat.ecommerce_backend.modules.payment.entity.Payment;
import com.duynhat.ecommerce_backend.modules.payment.enums.PaymentMethod;
import com.duynhat.ecommerce_backend.modules.payment.enums.PaymentStatus;
import com.duynhat.ecommerce_backend.modules.payment.gateway.vnpay.VnPayPaymentUrlBuilder;
import com.duynhat.ecommerce_backend.modules.payment.gateway.vnpay.VnPayReturnVerifier;
import com.duynhat.ecommerce_backend.modules.payment.impl.PaymentServiceImpl;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplVnPayTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VnPayPaymentUrlBuilder vnPayPaymentUrlBuilder;

    @Mock
    private VnPayReturnVerifier vnPayReturnVerifier;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                paymentRepository,
                orderRepository,
                userRepository,
                vnPayPaymentUrlBuilder,
                vnPayReturnVerifier
        );
    }

    @Test
    void processVnPayReturn_success_shouldConfirmOrder() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("150000"));

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setOrder(order);
        payment.setAmount(new BigDecimal("150000"));
        payment.setMethod(PaymentMethod.VNPAY);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setMerchantTxnRef("VNP123456");

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "VNP123456");
        params.put("vnp_Amount", "15000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TransactionNo", "14567890");
        params.put("vnp_SecureHash", "valid-signature");

        when(vnPayReturnVerifier.isValid(params))
                .thenReturn(true);

        when(paymentRepository.findByMerchantTxnRefForUpdate("VNP123456"))
                .thenReturn(Optional.of(payment));

        when(paymentRepository.save(payment))
                .thenReturn(payment);

        VnPayReturnResult result =
                paymentService.processVnPayReturn(params);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.getPaymentId()).isEqualTo(paymentId);
        assertThat(result.getOrderId()).isEqualTo(orderId);

        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.SUCCESS);

        assertThat(order.getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);

        assertThat(payment.getGatewayResponseCode())
                .isEqualTo("00");

        assertThat(payment.getGatewayTransactionNo())
                .isEqualTo("14567890");

        verify(paymentRepository).save(payment);
    }

    @Test
    void processVnPayReturn_invalidSignature_shouldReject() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "VNP123456");
        params.put("vnp_Amount", "15000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_SecureHash", "invalid-signature");

        when(vnPayReturnVerifier.isValid(params))
                .thenReturn(false);

        assertThatThrownBy(
                () -> paymentService.processVnPayReturn(params)
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid VNPay signature");

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void processVnPayReturn_failed_shouldKeepOrderPending() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("200000"));

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setOrder(order);
        payment.setAmount(new BigDecimal("200000"));
        payment.setMethod(PaymentMethod.VNPAY);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setMerchantTxnRef("VNPFAILED123");

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "VNPFAILED123");
        params.put("vnp_Amount", "20000000");
        params.put("vnp_ResponseCode", "24");
        params.put("vnp_TransactionStatus", "02");
        params.put("vnp_TransactionNo", "987654321");
        params.put("vnp_SecureHash", "valid-signature");

        when(vnPayReturnVerifier.isValid(params))
                .thenReturn(true);

        when(paymentRepository.findByMerchantTxnRefForUpdate("VNPFAILED123"))
                .thenReturn(Optional.of(payment));

        when(paymentRepository.save(payment))
                .thenReturn(payment);

        VnPayReturnResult result =
                paymentService.processVnPayReturn(params);

        assertThat(result.isSuccess()).isFalse();

        assertThat(result.getStatus())
                .isEqualTo(PaymentStatus.FAILED);

        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.FAILED);

        assertThat(order.getStatus())
                .isEqualTo(OrderStatus.PENDING);

        assertThat(payment.getGatewayResponseCode())
                .isEqualTo("24");

        assertThat(payment.getGatewayTransactionNo())
                .isEqualTo("987654321");

        assertThat(payment.getFailureReason())
                .isEqualTo(
                        "VNPay payment failed with response code 24"
                );

        verify(paymentRepository).save(payment);
    }

    @Test
    void processVnPayReturn_alreadySuccessful_shouldBeIdempotent() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setTotalAmount(new BigDecimal("300000"));

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setOrder(order);
        payment.setAmount(new BigDecimal("300000"));
        payment.setMethod(PaymentMethod.VNPAY);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setMerchantTxnRef("VNPIDEMPOTENT123");
        payment.setGatewayResponseCode("00");
        payment.setGatewayTransactionNo("111222333");

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "VNPIDEMPOTENT123");
        params.put("vnp_Amount", "30000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TransactionNo", "111222333");
        params.put("vnp_SecureHash", "valid-signature");

        when(vnPayReturnVerifier.isValid(params))
                .thenReturn(true);

        when(paymentRepository.findByMerchantTxnRefForUpdate(
                "VNPIDEMPOTENT123"
        )).thenReturn(Optional.of(payment));

        VnPayReturnResult result =
                paymentService.processVnPayReturn(params);

        assertThat(result.isSuccess()).isTrue();

        assertThat(result.getStatus())
                .isEqualTo(PaymentStatus.SUCCESS);

        assertThat(result.getPaymentId())
                .isEqualTo(paymentId);

        assertThat(result.getOrderId())
                .isEqualTo(orderId);

        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.SUCCESS);

        assertThat(order.getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);

        verify(paymentRepository, never())
                .save(any(Payment.class));
    }
}