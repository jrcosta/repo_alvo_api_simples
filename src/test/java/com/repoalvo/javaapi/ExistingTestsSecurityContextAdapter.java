package com.repoalvo.javaapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * This class adapts existing tests to run with security context,
 * preventing false negatives after Spring Security dependency inclusion.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ExistingTestsSecurityContextAdapter {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void whenAccessHealthEndpointWithoutAuthentication_thenOk() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk());
    }

    @Test
    public void whenAccessUsersWithoutAuthentication_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/users"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void whenAccessUsersWithAuthentication_thenOk() throws Exception {
        mockMvc.perform(get("/users"))
            .andExpect(status().isOk());
    }
}