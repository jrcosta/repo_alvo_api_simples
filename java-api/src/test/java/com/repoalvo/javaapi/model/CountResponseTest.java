package com.repoalvo.javaapi.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.assertj.core.api.Assertions.*;

class CountResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should create CountResponse with count and resource and validate fields")
    void shouldCreateCountResponseWithCountAndResource() {
        int count = 5;
        String resource = "users";

        CountResponse response = new CountResponse(count, resource);

        assertThat(response.count()).isEqualTo(count);
        assertThat(response.resource()).isEqualTo(resource);
    }

    @Test
    @DisplayName("Should create CountResponse with only count and resource defaults to 'users'")
    void shouldCreateCountResponseWithCountOnlyAndDefaultResource() {
        int count = 10;

        CountResponse response = new CountResponse(count);

        assertThat(response.count()).isEqualTo(count);
        assertThat(response.resource()).isEqualTo("users");
    }

    @Test
    @DisplayName("Should serialize CountResponse to JSON including both fields")
    void shouldSerializeCountResponseToJson() throws JsonProcessingException {
        CountResponse response = new CountResponse(7, "users");

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"count\":7");
        assertThat(json).contains("\"resource\":\"users\"");
    }

    @Test
    @DisplayName("Should deserialize JSON with both count and resource fields")
    void shouldDeserializeJsonWithCountAndResource() throws JsonProcessingException {
        String json = "{\"count\":3,\"resource\":\"users\"}";

        CountResponse response = objectMapper.readValue(json, CountResponse.class);

        assertThat(response.count()).isEqualTo(3);
        assertThat(response.resource()).isEqualTo("users");
    }

    @Test
    @DisplayName("Should deserialize JSON with only count field and resource defaults to null")
    void shouldDeserializeJsonWithOnlyCountField() throws JsonProcessingException {
        String json = "{\"count\":4}";

        CountResponse response = objectMapper.readValue(json, CountResponse.class);

        assertThat(response.count()).isEqualTo(4);
        // Since resource is missing, Jackson sets it to null (no default constructor logic applied)
        assertThat(response.resource()).isNull();
    }

    @Test
    @DisplayName("Should consider CountResponse objects equal if count and resource are equal")
    void shouldHaveConsistentEqualsAndHashCode() {
        CountResponse a = new CountResponse(2, "users");
        CountResponse b = new CountResponse(2, "users");
        CountResponse c = new CountResponse(2, "other");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());

        assertThat(a).isNotEqualTo(c);
        assertThat(a.hashCode()).isNotEqualTo(c.hashCode());
    }

    @Test
    @DisplayName("Should include both fields in toString output")
    void shouldIncludeFieldsInToString() {
        CountResponse response = new CountResponse(8, "users");

        String toString = response.toString();

        assertThat(toString).contains("count=8");
        assertThat(toString).contains("resource=users");
    }
}