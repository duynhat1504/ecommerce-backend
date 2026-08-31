package com.duynhat.ecommerce_backend.integration.user;

import com.duynhat.ecommerce_backend.integration.AbstractIntegrationTest;
import com.duynhat.ecommerce_backend.modules.auth.RefreshTokenRepository;
import com.duynhat.ecommerce_backend.modules.auth.entity.RefreshToken;
import com.duynhat.ecommerce_backend.modules.auth.jwt.JwtService;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import com.duynhat.ecommerce_backend.modules.user.enums.Role;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
class AdminUserIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void listUsers_withAdminRole_shouldReturnPagedUsersWithoutSensitiveFields() throws Exception {
        User admin = createUser(
                "admin-list@example.com",
                "Admin List",
                Role.ADMIN,
                true
        );
        createUser("customer-list@example.com", "Customer List", Role.USER, true);

        mockMvc.perform(
                        get("/api/admin/users")
                                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Get users successfully"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].id").isNotEmpty())
                .andExpect(jsonPath("$.data.content[0].email").isNotEmpty())
                .andExpect(jsonPath("$.data.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].refreshToken").doesNotExist());
    }

    @Test
    void listUsers_withUserRole_shouldReturn403() throws Exception {
        User user = createUser(
                "plain-user-access@example.com",
                "Plain User",
                Role.USER,
                true
        );

        mockMvc.perform(
                        get("/api/admin/users")
                                .header(HttpHeaders.AUTHORIZATION, bearer(user))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("You do not have permission to access this resource"));
    }

    @Test
    void listUsers_withoutAuthentication_shouldReturn401() throws Exception {
        mockMvc.perform(
                        get("/api/admin/users")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void listUsers_withFiltersAndPagination_shouldReturnMatchingPage() throws Exception {
        User admin = createUser(
                "admin-filter@example.com",
                "Admin Filter",
                Role.ADMIN,
                true
        );
        createUser("alice-buyer@example.com", "Alice Buyer", Role.USER, true);
        createUser("bob-buyer@example.com", "Bob Buyer", Role.USER, true);
        createUser("inactive-buyer@example.com", "Inactive Buyer", Role.USER, false);
        createUser("admin-buyer@example.com", "Admin Buyer", Role.ADMIN, true);

        mockMvc.perform(
                        get("/api/admin/users")
                                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                                .param("keyword", "buyer")
                                .param("role", "USER")
                                .param("active", "true")
                                .param("page", "1")
                                .param("size", "1")
                                .param("sort", "email,asc")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].email")
                        .value("bob-buyer@example.com"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.first").value(false))
                .andExpect(jsonPath("$.data.last").value(true))
                .andExpect(jsonPath("$.data.numberOfElements").value(1));
    }

    @Test
    void getUserDetail_withAdminRole_shouldReturnAdminSafeFields() throws Exception {
        User admin = createUser(
                "admin-detail@example.com",
                "Admin Detail",
                Role.ADMIN,
                true
        );
        User user = createUser(
                "customer-detail@example.com",
                "Customer Detail",
                Role.USER,
                true
        );

        mockMvc.perform(
                        get("/api/admin/users/{id}", user.getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Get user successfully"))
                .andExpect(jsonPath("$.data.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.data.fullName").value("Customer Detail"))
                .andExpect(jsonPath("$.data.email").value("customer-detail@example.com"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.emailVerified").value(true))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void updateStatus_shouldDeactivateAndActivateNormalUser() throws Exception {
        User admin = createUser(
                "admin-status@example.com",
                "Admin Status",
                Role.ADMIN,
                true
        );
        User user = createUser(
                "customer-status@example.com",
                "Customer Status",
                Role.USER,
                true
        );

        mockMvc.perform(
                        put("/api/admin/users/{id}/status", user.getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "active": false
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Update user status successfully"))
                .andExpect(jsonPath("$.data.active").value(false));

        assertThat(userRepository.findById(user.getId()).orElseThrow().getActive())
                .isFalse();

        mockMvc.perform(
                        put("/api/admin/users/{id}/status", user.getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "active": true
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true));

        assertThat(userRepository.findById(user.getId()).orElseThrow().getActive())
                .isTrue();
    }

    @Test
    void updateRole_shouldPromoteUserToAdmin() throws Exception {
        User admin = createUser(
                "admin-promote@example.com",
                "Admin Promote",
                Role.ADMIN,
                true
        );
        User user = createUser(
                "customer-promote@example.com",
                "Customer Promote",
                Role.USER,
                true
        );

        mockMvc.perform(
                        put("/api/admin/users/{id}/role", user.getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "role": "ADMIN"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Update user role successfully"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        assertThat(userRepository.findById(user.getId()).orElseThrow().getRole())
                .isEqualTo(Role.ADMIN);
    }

    @Test
    void updateRole_shouldDemoteAdminToUser() throws Exception {
        User admin = createUser(
                "admin-demote-actor@example.com",
                "Admin Demote Actor",
                Role.ADMIN,
                true
        );
        User targetAdmin = createUser(
                "admin-demote-target@example.com",
                "Admin Demote Target",
                Role.ADMIN,
                true
        );

        mockMvc.perform(
                        put("/api/admin/users/{id}/role", targetAdmin.getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "role": "USER"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"));

        assertThat(userRepository.findById(targetAdmin.getId()).orElseThrow().getRole())
                .isEqualTo(Role.USER);
    }

    @Test
    void updateStatus_whenAdminDeactivatesSelf_shouldReturn403() throws Exception {
        User admin = createUser(
                "admin-self-status@example.com",
                "Admin Self Status",
                Role.ADMIN,
                true
        );
        createUser("other-admin-status@example.com", "Other Admin", Role.ADMIN, true);

        mockMvc.perform(
                        put("/api/admin/users/{id}/status", admin.getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "active": false
                                        }
                                        """)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Admin cannot deactivate their own account"));
    }

    @Test
    void updateRole_whenAdminDemotesSelf_shouldReturn403() throws Exception {
        User admin = createUser(
                "admin-self-role@example.com",
                "Admin Self Role",
                Role.ADMIN,
                true
        );
        createUser("other-admin-role@example.com", "Other Admin", Role.ADMIN, true);

        mockMvc.perform(
                        put("/api/admin/users/{id}/role", admin.getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "role": "USER"
                                        }
                                        """)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Admin cannot demote their own account"));
    }

    @Test
    void updateStatus_whenLastActiveAdminWouldBeDeactivated_shouldReturn400() throws Exception {
        User admin = createUser(
                "admin-last-status@example.com",
                "Admin Last Status",
                Role.ADMIN,
                true
        );

        mockMvc.perform(
                        put("/api/admin/users/{id}/status", admin.getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "active": false
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("At least one active admin must remain"));
    }

    @Test
    void updateRole_whenLastActiveAdminWouldBeDemoted_shouldReturn400() throws Exception {
        User admin = createUser(
                "admin-last-role@example.com",
                "Admin Last Role",
                Role.ADMIN,
                true
        );

        mockMvc.perform(
                        put("/api/admin/users/{id}/role", admin.getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "role": "USER"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("At least one active admin must remain"));
    }

    @Test
    void getUserDetail_whenUserDoesNotExist_shouldReturn404() throws Exception {
        User admin = createUser(
                "admin-missing@example.com",
                "Admin Missing",
                Role.ADMIN,
                true
        );

        mockMvc.perform(
                        get("/api/admin/users/{id}", UUID.randomUUID())
                                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void deactivation_shouldRejectLoginAndRevokeExistingSessions() throws Exception {
        User admin = createUser(
                "admin-revoke@example.com",
                "Admin Revoke",
                Role.ADMIN,
                true
        );
        User user = createUser(
                "customer-revoke@example.com",
                "Customer Revoke",
                Role.USER,
                true
        );

        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "customer-revoke@example.com",
                                          "password": "secret123"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refresh_token"))
                .andReturn();

        String accessToken = JsonPath.read(
                loginResult.getResponse().getContentAsString(),
                "$.data.accessToken"
        );

        Cookie refreshCookie = loginResult.getResponse().getCookie("refresh_token");
        assertThat(refreshCookie).isNotNull();

        mockMvc.perform(
                        put("/api/admin/users/{id}/status", user.getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "active": false
                                        }
                                        """)
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "customer-revoke@example.com",
                                          "password": "secret123"
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        get("/api/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(refreshCookie)
                )
                .andExpect(status().isUnauthorized());

        List<RefreshToken> tokens = refreshTokenRepository
                .findAll()
                .stream()
                .filter(token -> token.getUser().getId().equals(user.getId()))
                .toList();

        assertThat(tokens).singleElement().matches(RefreshToken::isRevoked);
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateAccessToken(
                user.getEmail(),
                UUID.randomUUID(),
                LocalDateTime.now().plusDays(7)
        );
    }

    private User createUser(
            String email,
            String fullName,
            Role role,
            boolean active
    ) {
        return userRepository.saveAndFlush(
                User.builder()
                        .email(email)
                        .password(passwordEncoder.encode("secret123"))
                        .fullName(fullName)
                        .role(role)
                        .active(active)
                        .emailVerified(true)
                        .build()
        );
    }
}
