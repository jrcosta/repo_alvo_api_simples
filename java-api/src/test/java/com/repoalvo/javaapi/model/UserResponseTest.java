package com.repoalvo.javaapi.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateUserResponseWithAllFieldsAndGettersReturnCorrectValues() {
        // Arrange
        int id = 10;
        String name = "Test User";
        String email = "test.user@example.com";
        String status = "ACTIVE";
        String role = "ADMIN";

        // Act
        UserResponse userResponse = new UserResponse(id, name, email, status, role);

        // Assert
        assertThat(userResponse.id()).isEqualTo(id);
        assertThat(userResponse.name()).isEqualTo(name);
        assertThat(userResponse.email()).isEqualTo(email);
        assertThat(userResponse.status()).isEqualTo(status);
        assertThat(userResponse.role()).isEqualTo(role);
    }

    @Test
    void shouldSerializeToJsonIncludingStatusAndRole() throws JsonProcessingException {
        // Arrange
        UserResponse userResponse = new UserResponse(1, "Alice", "alice@example.com", "ACTIVE", "USER");

        // Act
        String json = objectMapper.writeValueAsString(userResponse);

        // Assert
        assertThat(json).contains("\"id\":1");
        assertThat(json).contains("\"name\":\"Alice\"");
        assertThat(json).contains("\"email\":\"alice@example.com\"");
        assertThat(json).contains("\"status\":\"ACTIVE\"");
        assertThat(json).contains("\"role\":\"USER\"");
    }

    @Test
    void shouldDeserializeFromJsonIncludingStatusAndRole() throws JsonProcessingException {
        // Arrange
        String json = """
                {
                  "id": 2,
                  "name": "Bob",
                  "email": "bob@example.com",
                  "status": "INACTIVE",
                  "role": "USER"
                }
                """;

        // Act
        UserResponse userResponse = objectMapper.readValue(json, UserResponse.class);

        // Assert
        assertThat(userResponse.id()).isEqualTo(2);
        assertThat(userResponse.name()).isEqualTo("Bob");
        assertThat(userResponse.email()).isEqualTo("bob@example.com");
        assertThat(userResponse.status()).isEqualTo("INACTIVE");
        assertThat(userResponse.role()).isEqualTo("USER");
    }

    @Test
    void shouldSerializeAndDeserializeWithExtremeValues() throws JsonProcessingException {
        // Arrange
        String longStatus = "A".repeat(1000);
        String specialRole = "!@#$%^&*()_+|";

        UserResponse userResponse = new UserResponse(3, "Charlie", "charlie@example.com", longStatus, specialRole);

        // Act
        String json = objectMapper.writeValueAsString(userResponse);
        UserResponse deserialized = objectMapper.readValue(json, UserResponse.class);

        // Assert
        assertThat(deserialized.status()).isEqualTo(longStatus);
        assertThat(deserialized.role()).isEqualTo(specialRole);
    }

    @Test
    void shouldThrowExceptionWhenDeserializingJsonMissingRequiredFields() {
        // Arrange
        String jsonMissingStatus = """
                {
                  "id": 4,
                  "name": "David",
                  "email": "david@example.com",
                  "role": "USER"
                }
                """;

        String jsonMissingRole = """
                {
                  "id": 5,
                  "name": "Eve",
                  "email": "eve@example.com",
                  "status": "ACTIVE"
                }
                """;

        // Act & Assert
        assertThatThrownBy(() -> objectMapper.readValue(jsonMissingStatus, UserResponse.class))
                .isInstanceOf(JsonProcessingException.class);

        assertThatThrownBy(() -> objectMapper.readValue(jsonMissingRole, UserResponse.class))
                .isInstanceOf(JsonProcessingException.class);
    }
}