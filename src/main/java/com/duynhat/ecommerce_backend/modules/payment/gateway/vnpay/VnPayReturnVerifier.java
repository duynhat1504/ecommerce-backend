package com.duynhat.ecommerce_backend.modules.payment.gateway.vnpay;

import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
public class VnPayReturnVerifier {

    private final VnPaySigner signer;

    public VnPayReturnVerifier(VnPaySigner signer) {
        this.signer = signer;
    }

    public boolean isValid(Map<String, String> params) {
        String secureHash = params.get("vnp_SecureHash");

        if (secureHash == null || secureHash.isBlank()) {
            return false;
        }

        Map<String, String> signedParams = new TreeMap<>(params);

        signedParams.remove("vnp_SecureHash");
        signedParams.remove("vnp_SecureHashType");

        String hashData = signedParams.entrySet()
                .stream()
                .filter(entry ->
                        entry.getValue() != null
                                && !entry.getValue().isBlank()
                )
                .map(entry ->
                        entry.getKey()
                                + "="
                                + encode(entry.getValue())
                )
                .collect(Collectors.joining("&"));

        String calculatedHash = signer.sign(hashData);

        return calculatedHash.equalsIgnoreCase(secureHash);
    }

    private String encode(String value) {
        return URLEncoder.encode(
                value,
                StandardCharsets.US_ASCII
        );
    }
}