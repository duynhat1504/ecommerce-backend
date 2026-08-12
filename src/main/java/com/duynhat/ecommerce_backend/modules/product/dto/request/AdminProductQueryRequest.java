package com.duynhat.ecommerce_backend.modules.product.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminProductQueryRequest {

    private Boolean active;
    private Integer page = 0;
    private Integer size = 10;
    private String sort = "createdAt,desc";
}
