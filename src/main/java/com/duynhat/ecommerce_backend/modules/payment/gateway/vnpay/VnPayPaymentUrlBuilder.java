package com.duynhat.ecommerce_backend.modules.payment.gateway.vnpay;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.modules.payment.entity.Payment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
public class VnPayPaymentUrlBuilder {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final DateTimeFormatter VN_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnPayProperties properties;

    private final VnPaySigner signer;

    public VnPayPaymentUrlBuilder(
            VnPayProperties properties,
            VnPaySigner signer
    ) {
        this.properties = properties;
        this.signer = signer;
    }

    public String build(Payment payment, String clientIp) {
        validateConfiguration();

        ZonedDateTime now = ZonedDateTime.now(VN_ZONE);

        ZonedDateTime expireAt = now.plusMinutes(properties.getExpireMinutes());

        Map<String, String> params = new TreeMap<>();

        params.put("vn_Version", "2.1.0");

        params.put("vnp_TmnCode", properties.getTmnCode());

        params.put(
                "vnp_Amount",
                toVnPayAmount(
                        payment.getAmount()
                )
        );

        params.put("vnp_CurrCode", "VND");

        params.put("vnp_TxnRef", payment.getMerchantTxnRef());

        params.put("vnp_OrderInfo",
                "Thanh toan don hang "
                        + payment
                        .getOrder()
                        .getOrderCode()
        );

        params.put("vnp_OrderType", "other");

        params.put("vnp_Locale", "vn");

        params.put("vnp_ReturnUrl", properties.getReturnUrl());

        params.put("vnp_IpAddr", clientIp);

        params.put("vnp_CreateDate", VN_DATE_FORMAT.format(now));

        params.put("vnp_ExpireDate",
                VN_DATE_FORMAT.format(expireAt));

        String hashData = buildQueryString(params);

        String secureHash = signer.sign(hashData);

        return properties.getPayUrl()
                + "?"
                + hashData
                + "&vnp_SecureHash="
                + secureHash;
    }

    private String buildQueryString(Map<String, String> params) {
        return params.entrySet()
                .stream()
                .filter(entry ->
                        entry.getValue() != null
                                && !entry.getValue().isBlank()
                )
                .map(entry ->
                        entry.getKey()
                                + "="
                                + encode(
                                entry.getValue()
                        )
                )
                .collect(Collectors.joining("&"));
    }

    private String encode(
            String value
    ) {
        return URLEncoder.encode(
                value,
                StandardCharsets.US_ASCII
        );
    }

    private String toVnPayAmount(BigDecimal amount) {

        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Invalid payment amount");
        }

        try {
            BigDecimal vnPayAmount = amount.movePointRight(2);

            String result = vnPayAmount
                    .toBigIntegerExact()
                    .toString();

            if (result.length() > 12) {
                throw new BadRequestException("Payment amount exceeds VNPay limit");
            }

            return result;

        } catch (ArithmeticException ex) {
            throw new BadRequestException("Invalid VNPay amount");
        }
    }

    private void validateConfiguration() {
        if (properties.getTmnCode() == null
                || properties.getTmnCode().isBlank()) {
            throw new IllegalStateException("VNPay TMN code is not configured");
        }

        if (properties.getHashSecret() == null
                || properties.getHashSecret().isBlank()) {
            throw new IllegalStateException("VNPay hash secret is not configured");
        }

        if (properties.getReturnUrl() == null
                || properties.getReturnUrl().isBlank()) {
            throw new IllegalStateException("VNPay return URL is not configured");
        }
    }
}
