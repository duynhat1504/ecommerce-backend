package com.duynhat.ecommerce_backend.modules.product.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ProductQueryRequest {

    private String keyword;
    private UUID categoryId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean active;
    private Integer page = 0;
    private Integer size = 10;
    private String sort = "createdAt,desc";
}
