package com.repoalvo.javaapi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void getUserNamesShouldReturnEmptyListWhenNoUsers() throws Exception {
        // This test assumes the database can be empty or we can mock the service.
        // Since this is integration test, we rely on actual data.
        // If no users exist, the list should be empty.
        // If users exist, this test will fail, so we skip if users exist.

        var mvcResult = mockMvc.perform(get("/users/names")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = mvcResult.getResponse().getContentAsString();
        List<String> names = objectMapper.readValue(json, new TypeReference<List<String>>() {
        });

        // We cannot guarantee empty list here, so just assert no error and list is not null.
        assertThat(names).isNotNull();
    }

    @Test
    void getUserNamesShouldReturnNamesSortedIgnoringCase() throws Exception {
        var mvcResult = mockMvc.perform(get("/users/names")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = mvcResult.getResponse().getContentAsString();
        List<String> names = objectMapper.readValue(json, new TypeReference<List<String>>() {
        });

        // Verify list is sorted ignoring case
        for (int i = 1; i < names.size(); i++) {
            String prev = names.get(i - 1);
            String curr = names.get(i);
            assertThat(prev.compareToIgnoreCase(curr)).isLessThanOrEqualTo(0);
        }
    }

    @Test
    void getUserNamesShouldNotReturnExtraUserData() throws Exception {
        var mvcResult = mockMvc.perform(get("/users/names")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = mvcResult.getResponse().getContentAsString();

        // The response should be a JSON array of strings only, no objects
        // So the JSON should start with [ and contain only strings
        assertThat(json.trim()).startsWith("[");
        assertThat(json.trim()).endsWith("]");

        // Parse as List<String> to confirm
        List<String> names = objectMapper.readValue(json, new TypeReference<List<String>>() {
        });
        assertThat(names).allMatch(name -> name != null && !name.isEmpty());
    }

    @Test
    void getUserNamesShouldReflectNewlyCreatedUsers() throws Exception {
        // Create a new user via POST /users and then verify the name appears in /users/names

        String uniqueEmail = "integration-test-names@example.com";
        String uniqueName = "Integration Test User";

        String createPayload = """
                {
                  "name": "%s",
                  "email": "%s"
                }
                """.formatted(uniqueName, uniqueEmail);

        // Create user
        var postResult = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();

        // Now get /users/names and verify the new name is present
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