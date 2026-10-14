package com.duynhat.ecommerce_backend.modules.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardLowStockProductResponse {

    private UUID id;
    private String name;
    private Integer stock;
    private Boolean active;
}
