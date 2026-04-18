package com.repoalvo.javaapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repoalvo.javaapi.model.UserExistsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Existing tests omitted for brevity...

    @Test
    void userExistsEndpointShouldReturnTrueForExistingUser() throws Exception {
        // First create a user to ensure existence
        String uniqueEmail = "exists-true@example.com";
        String createPayload = """
                {
                  "name": "Exists True",
                  "email": "%s"
                }
                """.formatted(uniqueEmail);

        String createdBody = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(createdBody);
        int createdId = created.get("id").asInt();

        mockMvc.perform(get("/users/{userId}/exists", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists", is(true)));
    }

    @Test
    void userExistsEndpointShouldReturnFalseForNonExistingUser() throws Exception {
        // Use a very high userId unlikely to exist
        int nonExistingUserId = 9999999;

        mockMvc.perform(get("/users/{userId}/exists", nonExistingUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists", is(false)));
    }

    @Test
    void userExistsEndpointShouldReturn200AndCorrectJsonStructure() throws Exception {
        // Create user to test positive case
        String uniqueEmail = "exists-structure@example.com";
        String createPayload = """
                {
                  "name": "Exists Structure",
                  "email": "%s"
                }
                """.formatted(uniqueEmail);

        String createdBody = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(createdBody);
        int createdId = created.get("id").asInt();

        mockMvc.perform(get("/users/{userId}/exists", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").exists())
                .andExpect(jsonPath("$.exists").isBoolean());
    }
}