package com.repoalvo.javaapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerDeleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("DELETE /users/{userId} returns 204 when user exists")
    void deleteUserShouldReturn204WhenUserExists() throws Exception {
        int userId = 1;

        // Verify user exists first
        mockMvc.perform(get("/users/" + userId))
                .andExpect(status().isOk());

        // Delete user
        mockMvc.perform(delete("/users/" + userId))
                .andExpect(status().isNoContent());

        // Verify user no longer exists
        mockMvc.perform(get("/users/" + userId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /users/{userId} returns 404 when user does not exist")
    void deleteUserShouldReturn404WhenUserDoesNotExist() throws Exception {
        int userId = 999;

        mockMvc.perform(delete("/users/" + userId))
                .andExpect(status().isNotFound());
    }
}
