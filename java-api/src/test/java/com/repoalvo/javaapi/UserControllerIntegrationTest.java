package com.repoalvo.javaapi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /health returns 200 and status ok")
    void getHealthShouldReturnStatusOk() throws Exception {
        mockMvc.perform(get("/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    @DisplayName("GET /users returns 200 and list of users")
    void getUsersShouldReturnListOfUsers() throws Exception {
        var mvcResult = mockMvc.perform(get("/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = mvcResult.getResponse().getContentAsString();
        List<Map<String, Object>> users = objectMapper.readValue(json, new TypeReference<>() {
        });

        assertThat(users).isNotEmpty();
        assertThat(users.get(0)).containsKeys("id", "name", "email");
    }

    @Test
    @DisplayName("GET /users/count returns 200 and count of users")
    void getUsersCountShouldReturnCount() throws Exception {
        var mvcResult = mockMvc.perform(get("/users/count")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = mvcResult.getResponse().getContentAsString();
        Map<String, Integer> countResponse = objectMapper.readValue(json, new TypeReference<>() {
        });

        assertThat(countResponse).containsKey("count");
        assertThat(countResponse.get("count")).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("GET /users/search?q= returns filtered users")
    void getUsersSearchShouldReturnFilteredUsers() throws Exception {
        var mvcResult = mockMvc.perform(get("/users/search")
                        .param("q", "ana")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = mvcResult.getResponse().getContentAsString();
        List<Map<String, Object>> users = objectMapper.readValue(json, new TypeReference<>() {
        });

        assertThat(users).isNotEmpty();
        for (Map<String, Object> user : users) {
            String name = (String) user.get("name");
            assertThat(name.toLowerCase()).contains("ana");
        }
    }

    @Test
    @DisplayName("GET /users/duplicates returns users with duplicate emails")
    void getUsersDuplicatesShouldReturnDuplicates() throws Exception {
        var mvcResult = mockMvc.perform(get("/users/duplicates")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = mvcResult.getResponse().getContentAsString();
        List<Map<String, Object>> duplicates = objectMapper.readValue(json, new TypeReference<>() {
        });

        // The list may be empty or contain duplicates, just verify structure
        for (Map<String, Object> user : duplicates) {
            assertThat(user).containsKeys("id", "name", "email");
        }
    }

    @Test
    @DisplayName("GET /users/1 returns 200 and the expected user")
    void getUserByIdShouldReturnUser() throws Exception {
        mockMvc.perform(get("/users/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Ana Silva"))
                .andExpect(jsonPath("$.email").value("ana@example.com"));
    }

    @Test
    @DisplayName("GET /users/9999 returns 404 when user does not exist")
    void getUserByIdShouldReturn404WhenNotFound() throws Exception {
        mockMvc.perform(get("/users/9999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /users returns 201 and the created user")
    @DirtiesContext
    void postUserShouldReturnCreatedUser() throws Exception {
        String payload = """
                {"name": "Carlos Souza", "email": "carlos.souza@example.com"}
                """;

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Carlos Souza"))
                .andExpect(jsonPath("$.email").value("carlos.souza@example.com"));
    }

    @Test
    @DisplayName("POST /users returns 409 CONFLICT when email already exists")
    void postUserShouldReturn409WhenEmailAlreadyExists() throws Exception {
        String payload = """
                {"name": "Ana Clone", "email": "ana@example.com"}
                """;

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }
}
