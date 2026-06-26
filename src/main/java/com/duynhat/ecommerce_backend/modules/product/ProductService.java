package com.duynhat.ecommerce_backend.modules.product;

import com.duynhat.ecommerce_backend.modules.product.dto.request.CreateProductRequest;
import com.duynhat.ecommerce_backend.modules.product.dto.request.UpdateProductRequest;
import com.duynhat.ecommerce_backend.modules.product.dto.response.ProductResponse;

import java.util.List;
import java.util.UUID;

public interface ProductService {

    ProductResponse create(CreateProductRequest request);
    List<ProductResponse> getAll();
    ProductResponse getById(UUID id);
    ProductResponse update(UUID id, UpdateProductRequest request);
}
