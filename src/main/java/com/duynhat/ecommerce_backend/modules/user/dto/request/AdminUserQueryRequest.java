package com.duynhat.ecommerce_backend.modules.user.dto.request;

import com.duynhat.ecommerce_backend.modules.user.enums.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUserQueryRequest {

    private String keyword;
    private Role role;
    private Boolean active;
    private Integer page = 0;
    private Integer size = 10;
    private String sort = "createdAt,desc";
}
