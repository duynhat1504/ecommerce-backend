package com.duynhat.ecommerce_backend.modules.user.impl;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.common.core.exception.ForbiddenException;
import com.duynhat.ecommerce_backend.common.core.exception.ResourceNotFoundException;
import com.duynhat.ecommerce_backend.modules.auth.AccessTokenBlacklistService;
import com.duynhat.ecommerce_backend.modules.auth.RefreshTokenService;
import com.duynhat.ecommerce_backend.modules.user.AdminUserService;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.dto.request.AdminUserQueryRequest;
import com.duynhat.ecommerce_backend.modules.user.dto.request.UpdateUserRoleRequest;
import com.duynhat.ecommerce_backend.modules.user.dto.request.UpdateUserStatusRequest;
import com.duynhat.ecommerce_backend.modules.user.dto.response.AdminUserResponse;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import com.duynhat.ecommerce_backend.modules.user.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "fullName",
            "email",
            "role",
            "active",
            "createdAt",
            "updatedAt"
    );

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private AccessTokenBlacklistService accessTokenBlacklistService;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> findUsers(AdminUserQueryRequest req) {
        int page = req.getPage() == null ? 0 : req.getPage();
        int size = req.getSize() == null ? 10 : req.getSize();

        validatePagination(page, size);

        String keyword = normalizeKeyword(req.getKeyword());
        Sort sort = buildSort(req.getSort());
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> users;

        if (keyword == null) {
            users = userRepository.filterAdminUsers(
                    req.getRole(),
                    req.getActive(),
                    pageable
            );
        } else {
            users = userRepository.searchAdminUsersByKeyword(
                    keyword,
                    req.getRole(),
                    req.getActive(),
                    pageable
            );
        }

        return users.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getById(UUID id) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return toResponse(user);
    }

    @Override
    @Transactional
    public AdminUserResponse updateStatus(UUID id, UpdateUserStatusRequest req) {
        if (req.getActive() == null) {
            throw new BadRequestException("Active status is required");
        }

        User currentAdmin = getCurrentUser();
        List<User> activeAdmins = lockActiveAdmins();

        User targetUser = getLockedTarget(id, activeAdmins);

        if (targetUser.getActive().equals(req.getActive())) {
            return toResponse(targetUser);
        }

        if (isActiveAdmin(targetUser) && Boolean.FALSE.equals(req.getActive())) {
            validateAnotherActiveAdminExists(activeAdmins, targetUser.getId());
        }

        if (currentAdmin.getId().equals(targetUser.getId())
                && Boolean.FALSE.equals(req.getActive())) {
            throw new ForbiddenException("Admin cannot deactivate their own account");
        }

        targetUser.setActive(req.getActive());
        User savedUser = userRepository.save(targetUser);

        if (Boolean.FALSE.equals(req.getActive())) {
            revokeUserSessions(savedUser.getId());
        }

        return toResponse(savedUser);
    }

    @Override
    @Transactional
    public AdminUserResponse updateRole(UUID id, UpdateUserRoleRequest req) {
        if (req.getRole() == null) {
            throw new BadRequestException("Role is required");
        }

        User currentAdmin = getCurrentUser();
        List<User> activeAdmins = lockActiveAdmins();

        User targetUser = getLockedTarget(id, activeAdmins);

        if (targetUser.getRole() == req.getRole()) {
            return toResponse(targetUser);
        }

        if (isActiveAdmin(targetUser) && req.getRole() == Role.USER) {
            validateAnotherActiveAdminExists(activeAdmins, targetUser.getId());
        }

        if (currentAdmin.getId().equals(targetUser.getId())
                && req.getRole() == Role.USER) {
            throw new ForbiddenException("Admin cannot demote their own account");
        }

        targetUser.setRole(req.getRole());

        return toResponse(userRepository.save(targetUser));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BadRequestException("User is not authenticated");
        }

        return userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private List<User> lockActiveAdmins() {
        return userRepository.findActiveUsersByRoleForUpdate(Role.ADMIN);
    }

    private User getLockedTarget(UUID userId, List<User> activeAdmins) {
        return activeAdmins
                .stream()
                .filter(user -> user.getId().equals(userId))
                .findFirst()
                .orElseGet(() ->
                        userRepository
                                .findByIdForUpdate(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                );
    }

    private void validateAnotherActiveAdminExists(
            List<User> activeAdmins,
            UUID targetUserId
    ) {
        boolean anotherActiveAdminExists = activeAdmins
                .stream()
                .anyMatch(user -> !user.getId().equals(targetUserId));

        if (!anotherActiveAdminExists) {
            throw new BadRequestException("At least one active admin must remain");
        }
    }

    private void revokeUserSessions(UUID userId) {
        Set<UUID> sessionIds = refreshTokenService.revokeAllRefreshTokens(userId);

        sessionIds.forEach(accessTokenBlacklistService::blacklistSession);
    }

    private boolean isActiveAdmin(User user) {
        return user.getRole() == Role.ADMIN
                && Boolean.TRUE.equals(user.getActive());
    }

    private Sort buildSort(String sortParam) {
        Sort sort;

        if (sortParam == null || sortParam.isBlank()) {
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        } else {
            String[] parts = sortParam.split(",");

            if (parts.length > 2) {
                throw new BadRequestException("Sort format must be field,direction");
            }

            String field = parts[0].trim();

            if (field.isBlank()) {
                throw new BadRequestException("Sort field must not be blank");
            }

            if (!ALLOWED_SORT_FIELDS.contains(field)) {
                throw new BadRequestException("Invalid sort field " + field);
            }

            String direction = parts.length == 2 ? parts[1].trim() : "asc";

            Sort.Direction sortDirection;

            try {
                sortDirection = Sort.Direction.fromString(direction);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Sort direction must be asc or desc");
            }

            sort = Sort.by(sortDirection, field);
        }

        return sort.and(Sort.by(Sort.Direction.ASC, "id"));
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page index must not be negative");
        }

        if (size <= 0 || size > 100) {
            throw new BadRequestException("Page size must be between 1 and 100");
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        String normalized = keyword.trim();

        if (normalized.length() > 100) {
            throw new BadRequestException("Keyword must not exceed 100 characters");
        }

        return normalized;
    }

    private AdminUserResponse toResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.getActive())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
