package com.duynhat.ecommerce_backend.modules.payment.gateway.vnpay;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component
public class VnPaySigner {

    private static final String HMAC_SHA512 = "HmacSHA512";

    private final VnPayProperties properties;

    public VnPaySigner(VnPayProperties properties) {
        this.properties = properties;
    }

    public String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA512);

            SecretKeySpec secretKey = new SecretKeySpec(
                    properties
                        .getHashSecret()
                        .getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA512
            );

            mac.init(secretKey);

            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            return HexFormat
                    .of()
                    .formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to generate VNPay signature",
                    ex
            );
        }
    }
}
