package com.repoalvo.javaapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.repoalvo.javaapi.model.UserCreateRequest;
import com.repoalvo.javaapi.model.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /users creates user with valid phoneNumber and returns 201 with phoneNumber in response")
    void createUserWithValidPhoneNumberShouldReturn201AndPhoneNumber() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Paula Mendes",
                "paula.mendes@example.com",
                "USER",
                "+55 11 90000-0001"
        );

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("Paula Mendes")))
                .andExpect(jsonPath("$.email", is("paula.mendes@example.com")))
                .andExpect(jsonPath("$.phoneNumber", is("+55 11 90000-0001")));
    }

    @Test
    @DisplayName("POST /users creates user with phoneNumber absent and returns 201 without phoneNumber field")
    void createUserWithoutPhoneNumberShouldReturn201() throws Exception {
        // JSON without phoneNumber field
        String jsonPayload = """
                {
                    "name": "Rafael Oliveira",
                    "email": "rafael.oliveira@example.com",
                    "role": "USER"
                }
                """;

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("Rafael Oliveira")))
                .andExpect(jsonPath("$.email", is("rafael.oliveira@example.com")))
                .andExpect(jsonPath("$.phoneNumber").doesNotExist());
    }

    @Test
    @DisplayName("POST /users creates user with empty phoneNumber and returns 201 with empty phoneNumber")
    void createUserWithEmptyPhoneNumberShouldReturn201() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Sofia Almeida",
                "sofia.almeida@example.com",
                "USER",
                ""
        );

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phoneNumber", is("")));
    }

    @Test
    @DisplayName("POST /users creates user with invalid phoneNumber and returns 201 with phoneNumber as is")
    void createUserWithInvalidPhoneNumberShouldReturn201() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Thiago Santos",
                "thiago.santos@example.com",
                "USER",
                "invalid-phone-123!@#"
        );

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phoneNumber", is("invalid-phone-123!@#")));
    }

    @Test
    @DisplayName("GET /users returns created user with phoneNumber present")
    void listUsersShouldIncludeCreatedUserWithPhoneNumber() throws Exception {
        // First create user with phoneNumber
        UserCreateRequest request = new UserCreateRequest(
                "Vanessa Ribeiro",
                "vanessa.ribeiro@example.com",
                "USER",
                "+55 41 91234-5678"
        );

        String responseContent = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UserResponse createdUser = objectMapper.readValue(responseContent, UserResponse.class);

        // Then list users and check if created user with phoneNumber is present
        mockMvc.perform(get("/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(createdUser.id())))
                .andExpect(jsonPath("$[?(@.id == %d)].phoneNumber", createdUser.id()).value(hasItem("+55 41 91234-5678")));
    }
}