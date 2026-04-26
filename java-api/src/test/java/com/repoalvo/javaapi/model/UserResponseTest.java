package com.repoalvo.javaapi.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateUserResponseWithAllFieldsIncludingPhoneNumber() {
        UserResponse user = new UserResponse(
                10,
                "Test User",
                "testuser@example.com",
                "ACTIVE",
                "USER",
                "+55 11 90000-0001"
        );

        assertThat(user.id()).isEqualTo(10);
        assertThat(user.name()).isEqualTo("Test User");
        assertThat(user.email()).isEqualTo("testuser@example.com");
        assertThat(user.status()).isEqualTo("ACTIVE");
        assertThat(user.role()).isEqualTo("USER");
        assertThat(user.phoneNumber()).isEqualTo("+55 11 90000-0001");
        assertThat(user.vip()).isFalse();
    }

    @Test
    void shouldCreateUserResponseWithOldConstructorAndPhoneNumberIsNull() {
        UserResponse user = new UserResponse(
                11,
                "Old User",
                "olduser@example.com",
                "INACTIVE",
                "ADMIN"
        );

        assertThat(user.id()).isEqualTo(11);
        assertThat(user.name()).isEqualTo("Old User");
        assertThat(user.email()).isEqualTo("olduser@example.com");
        assertThat(user.status()).isEqualTo("INACTIVE");
        assertThat(user.role()).isEqualTo("ADMIN");
        assertThat(user.phoneNumber()).isNull();
        assertThat(user.vip()).isTrue();
    }

    @Test
    void shouldSerializeUserResponseIncludingPhoneNumber() throws JsonProcessingException {
        UserResponse user = new UserResponse(
                20,
                "Serialize User",
                "serialize@example.com",
                "ACTIVE",
                "USER",
                "+55 99 99999-9999"
        );

        String json = objectMapper.writeValueAsString(user);

        assertThat(json).contains("\"phoneNumber\":\"+55 99 99999-9999\"");
        assertThat(json).contains("\"id\":20");
        assertThat(json).contains("\"name\":\"Serialize User\"");
        assertThat(json).contains("\"email\":\"serialize@example.com\"");
        assertThat(json).contains("\"vip\":false");
    }

    @Test
    void shouldSerializeUserResponseWithNullPhoneNumber() throws JsonProcessingException {
        UserResponse user = new UserResponse(
                21,
                "Null Phone",
                "nullphone@example.com",
                "ACTIVE",
                "USER",
                null
        );

        String json = objectMapper.writeValueAsString(user);

        // The field phoneNumber should appear with null value in JSON
        assertThat(json).contains("\"phoneNumber\":null");
    }

    @Test
    void shouldDeserializeUserResponseWithoutPhoneNumberField() throws JsonProcessingException {
        String jsonWithoutPhoneNumber = """
                {
                  "id": 30,
                  "name": "No Phone",
                  "email": "nophone@example.com",
                  "status": "ACTIVE",
                  "role": "USER"
                }
                """;

        UserResponse user = objectMapper.readValue(jsonWithoutPhoneNumber, UserResponse.class);

        assertThat(user.id()).isEqualTo(30);
        assertThat(user.name()).isEqualTo("No Phone");
        assertThat(user.email()).isEqualTo("nophone@example.com");
        assertThat(user.status()).isEqualTo("ACTIVE");
        assertThat(user.role()).isEqualTo("USER");
        // phoneNumber should be null when absent in JSON
        assertThat(user.phoneNumber()).isNull();
        assertThat(user.vip()).isFalse();
    }

    @Test
    void shouldCreateUserResponseWithPhoneNumberExplicitlyNull() {
        UserResponse user = new UserResponse(
                40,
                "Explicit Null",
                "explicitnull@example.com",
                "ACTIVE",
                "USER",
                null
        );

        assertThat(user.phoneNumber()).isNull();
    }

    @Test
    void shouldAllowExplicitVipValue() {
        UserResponse user = new UserResponse(
                50,
                "Manual Vip",
                "manualvip@example.com",
                "ACTIVE",
                "USER",
                null,
                true
        );

        assertThat(user.vip()).isTrue();
    }
}
