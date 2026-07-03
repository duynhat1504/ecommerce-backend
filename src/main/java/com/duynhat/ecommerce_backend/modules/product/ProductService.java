package com.duynhat.ecommerce_backend.modules.product;

import com.duynhat.ecommerce_backend.modules.product.dto.request.CreateProductRequest;
import com.duynhat.ecommerce_backend.modules.product.dto.request.UpdateProductRequest;
import com.duynhat.ecommerce_backend.modules.product.dto.response.ProductResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ProductService {

    ProductResponse create(CreateProductRequest request);
    Page<ProductResponse> getAll(int page, int size, String sort);
    Page<ProductResponse> searchFullText(String keyword, UUID categoryID, int page, int size);
    ProductResponse getById(UUID id);
    ProductResponse update(UUID id, UpdateProductRequest request);
}
