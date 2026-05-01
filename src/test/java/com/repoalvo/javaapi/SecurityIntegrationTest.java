package com.repoalvo.javaapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void whenAccessPublicEndpointWithoutAuthentication_thenOk() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk());
    }

    @Test
    public void whenAccessUsersEndpointWithoutAuthentication_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/users"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    public void whenAccessUsersEndpointWithAuthentication_thenOk() throws Exception {
        mockMvc.perform(get("/users"))
            .andExpect(status().isOk());
    }

    @Test
    public void whenAccessProtectedEndpointWithoutAuthentication_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/users/count"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void whenAccessProtectedEndpointWithAdminUser_thenOk() throws Exception {
        mockMvc.perform(get("/users/count"))
            .andExpect(status().isOk());
    }
}