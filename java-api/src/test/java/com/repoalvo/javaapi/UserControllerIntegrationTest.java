package com.repoalvo.javaapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.repoalvo.javaapi.service.UserService userService;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        userService.reset();
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /users/by-email returns 200 and user JSON when email exists")
    void getUserByEmailShouldReturnUserWhenEmailExists() throws Exception {
        String email = "ana@example.com";

        mockMvc.perform(get("/users/by-email")
                        .param("email", email)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").isString())
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.status").isString())
                .andExpect(jsonPath("$.role").isString())
                .andExpect(jsonPath("$", not(hasKey("password")))); // no sensitive data
    }

    @Test
    @DisplayName("GET /users/by-email returns 404 with message when email does not exist")
    void getUserByEmailShouldReturn404WhenEmailDoesNotExist() throws Exception {
        String email = "naoexiste@example.com";

        mockMvc.perform(get("/users/by-email")
                        .param("email", email)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.detail", containsString("Usuário não encontrado")));
    }

    @Test
    @DisplayName("GET /users/by-email returns 400 Bad Request when email parameter is missing")
    void getUserByEmailShouldReturn400WhenEmailParamMissing() throws Exception {
        mockMvc.perform(get("/users/by-email")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /users/by-email returns 404 when email parameter is empty")
    void getUserByEmailShouldReturn404WhenEmailParamEmpty() throws Exception {
        mockMvc.perform(get("/users/by-email")
                        .param("email", "")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString("Usuário não encontrado")));
    }

    @Test
    @DisplayName("GET /users/by-email handles email with special characters consistently")
    void getUserByEmailShouldHandleEmailWithSpecialCharacters() throws Exception {
        String email = "usuario+teste@example.com";

        // First try with user not existing
        mockMvc.perform(get("/users/by-email")
                        .param("email", email)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        // Optionally, if test data allows, create user with this email and test 200
        // But since no creation here, just test 404 is consistent
    }
}