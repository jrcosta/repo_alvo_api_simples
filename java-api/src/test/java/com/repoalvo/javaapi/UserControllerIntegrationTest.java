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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /users/names returns HTTP 200 and a JSON array of strings")
    void getUserNamesShouldReturnStatus200AndJsonArrayOfStrings() throws Exception {
        var mvcResult = mockMvc.perform(get("/users/names")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = mvcResult.getResponse().getContentAsString();
        List<String> names = objectMapper.readValue(json, new TypeReference<List<String>>() {
        });

        assertThat(names).isNotNull();
    }

    @Test
    @DisplayName("GET /users/names returns names sorted case-insensitively")
    void getUserNamesShouldReturnNamesSortedIgnoringCase() throws Exception {
        var mvcResult = mockMvc.perform(get("/users/names")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = mvcResult.getResponse().getContentAsString();
        List<String> names = objectMapper.readValue(json, new TypeReference<List<String>>() {
        });

        for (int i = 1; i < names.size(); i++) {
            String prev = names.get(i - 1);
            String curr = names.get(i);
            assertThat(prev.compareToIgnoreCase(curr)).isLessThanOrEqualTo(0);
        }
    }

    @Test
    @DisplayName("GET /users/names returns a flat JSON array of strings, not user objects")
    void getUserNamesShouldReturnFlatStringArray() throws Exception {
        var mvcResult = mockMvc.perform(get("/users/names")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = mvcResult.getResponse().getContentAsString();

        assertThat(json.trim()).startsWith("[");
        assertThat(json.trim()).endsWith("]");

        List<String> names = objectMapper.readValue(json, new TypeReference<List<String>>() {
        });
        assertThat(names).allSatisfy(name -> assertThat(name).isNotNull().isNotEmpty());
    }

    @Test
    @DisplayName("GET /users/names reflects a user created via POST /users")
    @DirtiesContext
    void getUserNamesShouldReflectNewlyCreatedUsers() throws Exception {
        String uniqueEmail = "integration-test-names@example.com";
        String uniqueName = "Integration Test User";

        String createPayload = """
                {
                  "name": "%s",
                  "email": "%s"
                }
                """.formatted(uniqueName, uniqueEmail);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated());

        var getResult = mockMvc.perform(get("/users/names")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = getResult.getResponse().getContentAsString();
        List<String> names = objectMapper.readValue(json, new TypeReference<List<String>>() {
        });

        assertThat(names).contains(uniqueName);
    }
}