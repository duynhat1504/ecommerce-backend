package com.duynhat.ecommerce_backend.modules.address.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateShippingAddressRequest {

    @NotBlank(message = "Recipient name is required")
    @Size(max = 150)
    private String recipientName;

    @NotBlank(message = "Phone number is required")
    @Size(max = 30)
    private String phoneNumber;

    @NotBlank(message = "Province is required")
    @Size(max = 100)
    private String province;

    @NotBlank(message = "District is required")
    @Size(max = 100)
    private String district;

    @NotBlank(message = "Ward is required")
    @Size(max = 100)
    private String ward;

    @NotBlank(message = "Address line is required")
    @Size(max = 255)
    private String addressLine;
}
