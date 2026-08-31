package com.duynhat.ecommerce_backend.modules.user;

import com.duynhat.ecommerce_backend.modules.user.entity.User;
import com.duynhat.ecommerce_backend.modules.user.enums.Role;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT u
        FROM User u
        WHERE u.id = :userId
        """)
    Optional<User> findByIdForUpdate(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT u
        FROM User u
        WHERE LOWER(u.email) = LOWER(:email)
        """)
    Optional<User> findByEmailIgnoreCaseForUpdate(@Param("email") String email);

    @Query("""
        SELECT u
        FROM User u
        WHERE (:role IS NULL OR u.role = :role)
          AND (:active IS NULL OR u.active = :active)
        """)
    Page<User> filterAdminUsers(
            @Param("role") Role role,
            @Param("active") Boolean active,
            Pageable pageable
    );

    @Query("""
        SELECT u
        FROM User u
        WHERE (:role IS NULL OR u.role = :role)
          AND (:active IS NULL OR u.active = :active)
          AND (
                LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
        """)
    Page<User> searchAdminUsersByKeyword(
            @Param("keyword") String keyword,
            @Param("role") Role role,
            @Param("active") Boolean active,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT u
        FROM User u
        WHERE u.role = :role
          AND u.active = true
        ORDER BY u.id ASC
        """)
    List<User> findActiveUsersByRoleForUpdate(@Param("role") Role role);
}
