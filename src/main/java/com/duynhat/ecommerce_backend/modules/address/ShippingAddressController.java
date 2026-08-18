package com.duynhat.ecommerce_backend.modules.address;


import com.duynhat.ecommerce_backend.common.core.dto.ApiResponse;
import com.duynhat.ecommerce_backend.modules.address.dto.request.CreateShippingAddressRequest;
import com.duynhat.ecommerce_backend.modules.address.dto.request.UpdateShippingAddressRequest;
import com.duynhat.ecommerce_backend.modules.address.dto.response.ShippingAddressResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Tag(
        name = "Shipping Address",
        description = "Shipping address management APIs"
)
@SecurityRequirement(name = "bearerAuth")
public class ShippingAddressController {
    private final ShippingAddressService addressService;

    @PostMapping
    @Operation(summary = "Create shipping address")
    public ResponseEntity<ApiResponse<ShippingAddressResponse>> create(
            @Valid @RequestBody CreateShippingAddressRequest request
    ) {
        ShippingAddressResponse response = addressService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Create shipping address successfully",
                                response
                        )
                );
    }

    @GetMapping
    @Operation(summary = "Get my shipping addresses")
    public ResponseEntity<ApiResponse<List<ShippingAddressResponse>>> getMyAddresses() {
        List<ShippingAddressResponse> addresses =
                addressService.getMyAddresses();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get shipping addresses successfully",
                        addresses
                )
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get my shipping address by id")
    public ResponseEntity<ApiResponse<ShippingAddressResponse>> getById(@PathVariable UUID id) {
        ShippingAddressResponse address = addressService.getMyAddressById(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Get shipping address successfully",
                        address
                )
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update shipping address")
    public ResponseEntity<ApiResponse<ShippingAddressResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateShippingAddressRequest request
    ) {
        ShippingAddressResponse address = addressService.update(id, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Update shipping address successfully",
                        address
                )
        );
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "Set shipping address as default")
    public ResponseEntity<ApiResponse<ShippingAddressResponse>> setDefault(
            @PathVariable UUID id
    ) {
        ShippingAddressResponse address = addressService.setDefault(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Set default shipping address successfully",
                        address
                )
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete shipping address")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        addressService.delete(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Delete shipping address successfully",
                        null
                )
        );
    }
}
