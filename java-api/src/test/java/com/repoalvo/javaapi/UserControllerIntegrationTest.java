package com.repoalvo.javaapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    void healthShouldReturnOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void usersCountShouldReturnInteger() throws Exception {
        mockMvc.perform(get("/users/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").isNumber());
    }

    @Test
    void listUsersShouldSupportPagination() throws Exception {
        mockMvc.perform(get("/users").param("limit", "1").param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    void createUserAndRejectDuplicateEmailFlow() throws Exception {
        String uniqueEmail = "java-integration@example.com";
        String createPayload = """
                {
                  "name": "Java Integration",
                  "email": "%s"
                }
                """.formatted(uniqueEmail);

        String createdBody = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(createdBody);
        int createdId = created.get("id").asInt();

        mockMvc.perform(get("/users/{userId}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(uniqueEmail));

        String duplicatePayload = """
                {
                  "name": "Outro Nome",
                  "email": "%s"
                }
                """.formatted(uniqueEmail);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicatePayload))
                .andExpect(status().isConflict());
    }

    @Test
    void searchAndEmailEndpointsShouldWork() throws Exception {
        mockMvc.perform(get("/users/search").param("q", "Ana"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/1/email"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@example.com"));
    }

    @Test
    void userExistsEndpointShouldReturnTrueAndFalse() throws Exception {
        mockMvc.perform(get("/users/1/exists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));

        mockMvc.perform(get("/users/999/exists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }

    @Test
    void userExistsEndpointShouldReturnTrueForCreatedUser() throws Exception {
        String uniqueEmail = "exists-true@example.com";
        String createPayload = """
                {
                  "name": "Exists True",
                  "email": "%s"
                }
                """.formatted(uniqueEmail);

        String createdBody = mockMvc.perform(post("/users")
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
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.exists").isBoolean());
    }
}