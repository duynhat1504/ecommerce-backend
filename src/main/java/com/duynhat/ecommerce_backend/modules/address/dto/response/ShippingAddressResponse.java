package com.duynhat.ecommerce_backend.modules.address.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ShippingAddressResponse {

    private UUID id;
    private String recipientName;
    private String phoneNumber;
    private String province;
    private String district;
    private String ward;
    private String addressLine;
    private String fullAddress;
    private Boolean defaultAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
