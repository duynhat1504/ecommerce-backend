package com.duynhat.ecommerce_backend.integration.auth;

import com.duynhat.ecommerce_backend.modules.auth.RefreshTokenRepository;
import com.duynhat.ecommerce_backend.modules.auth.entity.RefreshToken;
import com.jayway.jsonpath.JsonPath;
import com.duynhat.ecommerce_backend.integration.AbstractIntegrationTest;
import com.duynhat.ecommerce_backend.modules.auth.jwt.JwtService;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import com.duynhat.ecommerce_backend.modules.user.enums.Role;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

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

    @Test
    void refresh_withValidRefreshToken_shouldRotateTokenAndPreserveSession() throws Exception {
        User user = createUser("refresh@example.com", "secret123");

        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "email": "refresh@example.com",
                                      "password": "secret123"
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refresh_token"))
                .andReturn();

        String accessToken1 = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.data.accessToken");

        Cookie refreshCookie1 = loginResult.getResponse().getCookie("refresh_token");

        assertThat(refreshCookie1).isNotNull();

        MvcResult refreshResult = mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(refreshCookie1)
                )
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        String accessToken2 = JsonPath.read(refreshResult.getResponse().getContentAsString(), "$.data.accessToken");

        Cookie refreshCookie2 = refreshResult.getResponse().getCookie("refresh_token");

        assertThat(refreshCookie2).isNotNull();

        assertThat(refreshCookie2.getValue()).isNotEqualTo(refreshCookie1.getValue());
        assertThat(jwtService.extractSessionId(accessToken1))
                .isEqualTo(
                        jwtService.extractSessionId(accessToken2)
                );
        assertThat(jwtService.extractJti(accessToken1))
                .isNotEqualTo(
                        jwtService.extractJti(accessToken2)
                );

        List<RefreshToken> tokens =
                refreshTokenRepository.findAll()
                        .stream()
                        .filter(token ->
                                token.getUser()
                                        .getId()
                                        .equals(user.getId())
                        )
                        .toList();

        assertThat(tokens).hasSize(2);

        long revokedCount = tokens.stream().filter(RefreshToken::isRevoked).count();

        long activeCount = tokens.stream().filter(RefreshToken::isActive).count();

        assertThat(revokedCount).isEqualTo(1);
        assertThat(activeCount).isEqualTo(1);

        assertThat(
                tokens.stream()
                        .map(RefreshToken::getSessionId)
                        .distinct()
                        .count()
        ).isEqualTo(1);

        assertThat(tokens.getFirst().getSessionId())
                .isEqualTo(
                        jwtService.extractSessionId(accessToken2)
                );
    }

    @Test
    void logout_shouldRevokeWholeSessionAndRejectAllAccessTokens() throws Exception {
        createUser("logout-session@example.com", "secret123");

        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "email": "logout-session@example.com",
                                      "password": "secret123"
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refresh_token"))
                .andReturn();

        String accessToken1 = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.data.accessToken");

        Cookie refreshCookie1 = loginResult.getResponse().getCookie("refresh_token");

        assertThat(refreshCookie1).isNotNull();

        MvcResult refreshResult = mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(refreshCookie1)
                )
                .andExpect(status().isOk())
                .andReturn();

        String accessToken2 = JsonPath.read(refreshResult.getResponse().getContentAsString(), "$.data.accessToken");

        Cookie refreshCookie2 = refreshResult.getResponse().getCookie("refresh_token");

        assertThat(refreshCookie2).isNotNull();

        UUID sessionId = jwtService.extractSessionId(accessToken1);

        assertThat(jwtService.extractSessionId(accessToken2)).isEqualTo(sessionId);

        mockMvc.perform(
                        get("/api/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken1
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken2
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/auth/logout")
                                .cookie(refreshCookie2)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken2
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken1
                                )
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        get("/api/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken2
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_shouldOnlyRevokeCurrentSession() throws Exception {
        createUser("multi-session@example.com", "secret123");

        MvcResult loginResult1 = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "email": "multi-session@example.com",
                                      "password": "secret123"
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andReturn();

        String accessToken1 = JsonPath.read(loginResult1.getResponse().getContentAsString(), "$.data.accessToken");

        Cookie refreshCookie1 = loginResult1.getResponse().getCookie("refresh_token");

        assertThat(refreshCookie1).isNotNull();

        MvcResult loginResult2 = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "email": "multi-session@example.com",
                                      "password": "secret123"
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andReturn();

        String accessToken2 = JsonPath.read(loginResult2.getResponse().getContentAsString(), "$.data.accessToken");

        Cookie refreshCookie2 = loginResult2.getResponse().getCookie("refresh_token");

        assertThat(refreshCookie2).isNotNull();

        UUID sessionId1 = jwtService.extractSessionId(accessToken1);
        UUID sessionId2 = jwtService.extractSessionId(accessToken2);

        assertThat(sessionId1).isNotEqualTo(sessionId2);

        mockMvc.perform(
                        get("/api/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken1
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken2
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/auth/logout")
                                .cookie(refreshCookie1)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken1
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken1
                                )
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        get("/api/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken2
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    void logoutAll_shouldRevokeAllUserSessions()
            throws Exception {

        createUser(
                "logout-all@example.com",
                "secret123"
        );

        MvcResult loginResult1 = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "email": "logout-all@example.com",
                                      "password": "secret123"
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andReturn();

        String accessToken1 = JsonPath.read(loginResult1.getResponse().getContentAsString(), "$.data.accessToken");

        Cookie refreshCookie1 = loginResult1.getResponse().getCookie("refresh_token");

        assertThat(refreshCookie1).isNotNull();

        MvcResult loginResult2 = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "email": "logout-all@example.com",
                                      "password": "secret123"
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andReturn();

        String accessToken2 = JsonPath.read(loginResult2.getResponse().getContentAsString(), "$.data.accessToken");

        Cookie refreshCookie2 = loginResult2.getResponse().getCookie("refresh_token");

        assertThat(refreshCookie2).isNotNull();

        UUID sessionId1 = jwtService.extractSessionId(accessToken1);
        UUID sessionId2 = jwtService.extractSessionId(accessToken2);

        assertThat(sessionId1).isNotEqualTo(sessionId2);

        mockMvc.perform(
                        get("/api/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken1
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken2
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/auth/logout-all")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken1
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken1
                                )
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        get("/api/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken2
                                )
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(refreshCookie1)
                )
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(refreshCookie2)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_withAlreadyRotatedToken_shouldRejectReuse() throws Exception {
        createUser("refresh-reuse@example.com", "secret123");

        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "email": "refresh-reuse@example.com",
                                      "password": "secret123"
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie1 = loginResult.getResponse().getCookie("refresh_token");

        assertThat(refreshCookie1).isNotNull();

        MvcResult refreshResult = mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(refreshCookie1)
                )
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie2 = refreshResult.getResponse().getCookie("refresh_token");

        assertThat(refreshCookie2).isNotNull();
        assertThat(refreshCookie2.getValue()).isNotEqualTo(refreshCookie1.getValue());

        mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(refreshCookie1)
                )
                .andExpect(status().isBadRequest());

        List<RefreshToken> userTokens =
                refreshTokenRepository.findAll()
                        .stream()
                        .filter(token ->
                                token.getUser()
                                        .getEmail()
                                        .equals(
                                                "refresh-reuse@example.com"
                                        )
                        )
                        .toList();

        assertThat(userTokens).hasSize(2);
        assertThat(userTokens.stream()
                        .filter(RefreshToken::isRevoked)
                        .count()
        ).isEqualTo(1);
        assertThat(userTokens.stream()
                        .filter(RefreshToken::isActive)
                        .count()
        ).isEqualTo(1);
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
