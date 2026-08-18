package com.duynhat.ecommerce_backend.modules.address;

import com.duynhat.ecommerce_backend.modules.address.dto.request.CreateShippingAddressRequest;
import com.duynhat.ecommerce_backend.modules.address.dto.request.UpdateShippingAddressRequest;
import com.duynhat.ecommerce_backend.modules.address.dto.response.ShippingAddressResponse;

import java.util.List;
import java.util.UUID;

public interface ShippingAddressService {

    ShippingAddressResponse create(CreateShippingAddressRequest request);

    List<ShippingAddressResponse> getMyAddresses();

    ShippingAddressResponse getMyAddressById(UUID addressId);

    ShippingAddressResponse update(UUID addressId, UpdateShippingAddressRequest request);

    ShippingAddressResponse setDefault(UUID addressId);

    void delete(UUID addressId);
}
