package com.duynhat.ecommerce_backend.security;

import com.duynhat.ecommerce_backend.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class SecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getCurrentUser_withoutAuthentication_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    @WithMockUser(
            username = "usertest@gmail.com",
            roles = "USER"
    )
    void createProduct_withUserRole_shouldReturn403() throws Exception {
        mockMvc.perform(
                post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Test Product",
                                    "description": "Test",
                                    "price": 100,
                                    "stock": 10,
                                    "categoryId": "11111111-1111-1111-1111-111111111111"
                                }
                                """)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
    }

    @Test
    void getProducts_withoutToken_shouldNotReturn401() throws Exception {
        mockMvc.perform(
                get("/api/products")
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
    }
}
