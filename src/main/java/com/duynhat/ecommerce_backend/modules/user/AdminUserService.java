package com.duynhat.ecommerce_backend.modules.user;

import com.duynhat.ecommerce_backend.modules.user.dto.request.AdminUserQueryRequest;
import com.duynhat.ecommerce_backend.modules.user.dto.request.UpdateUserRoleRequest;
import com.duynhat.ecommerce_backend.modules.user.dto.request.UpdateUserStatusRequest;
import com.duynhat.ecommerce_backend.modules.user.dto.response.AdminUserResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface AdminUserService {

    Page<AdminUserResponse> findUsers(AdminUserQueryRequest req);
    AdminUserResponse getById(UUID id);
    AdminUserResponse updateStatus(UUID id, UpdateUserStatusRequest req);
    AdminUserResponse updateRole(UUID id, UpdateUserRoleRequest req);
}
