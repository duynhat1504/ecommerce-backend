package com.duynhat.ecommerce_backend.modules.payment.gateway.vnpay;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.vnpay")
public class VnPayProperties {

    private String payUrl;
    private String tmnCode;
    private String hashSecret;
    private String returnUrl;
    private int expireMinutes = 15;
}
