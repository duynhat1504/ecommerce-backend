package com.duynhat.ecommerce_backend.modules.category.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CategoryResponse {

    private UUID id;
    private String name;
    private String description;
    private Boolean active;
    private LocalDateTime deletedAt;
}
