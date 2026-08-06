package com.duynhat.ecommerce_backend.integration.auth;

import com.jayway.jsonpath.JsonPath;
import com.duynhat.ecommerce_backend.integration.AbstractIntegrationTest;
import com.duynhat.ecommerce_backend.modules.auth.jwt.JwtService;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import com.duynhat.ecommerce_backend.modules.user.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
@AutoConfigureMockMvc
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Test
    void register_withValidRequest_shouldCreateUser() throws Exception {
        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fullName": "Test User",
                                          "email": "TestUser@example.com",
                                          "password": "secret123"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Register successfully"))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("testuser@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Test User"))
                .andExpect(jsonPath("$.data.role").value("USER"));

        User savedUser = userRepository.findByEmail("testuser@example.com").orElseThrow();
        assertThat(savedUser.getPassword()).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", savedUser.getPassword())).isTrue();
        assertThat(savedUser.getActive()).isTrue();
    }

    @Test
    void register_withDuplicateEmail_shouldReturn409AndNotCreateAnotherUser() throws Exception {
        createUser("duplicate@example.com", "secret123");

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fullName": "Duplicate User",
                                          "email": "DUPLICATE@example.com",
                                          "password": "another-password"
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Data already exists or violates database constraints"));

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void login_withCorrectCredentials_shouldReturnValidAccessToken() throws Exception {
        User user = createUser("login@example.com", "secret123");

        MvcResult result = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "LOGIN@example.com",
                                          "password": "secret123"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(cookie().path("refresh_token", "/api/auth"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successfully"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.data.email").value("login@example.com"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andReturn();

        String accessToken = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.accessToken"
        );
        assertThat(jwtService.extractEmail(accessToken)).isEqualTo("login@example.com");
        assertThat(jwtService.isTokenValid(accessToken, "login@example.com")).isTrue();
    }

    @Test
    void login_withWrongPassword_shouldReturn401() throws Exception {
        createUser("login-failure@example.com", "secret123");

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "login-failure@example.com",
                                          "password": "wrong-password"
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    private User createUser(String email, String rawPassword) {
        return userRepository.saveAndFlush(
                User.builder()
                        .email(email)
                        .password(passwordEncoder.encode(rawPassword))
                        .fullName("Existing User")
                        .role(Role.USER)
                        .active(true)
                        .build()
        );
    }
}
