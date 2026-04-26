package com.repoalvo.javaapi;

import com.repoalvo.javaapi.model.UserCreateRequest;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class UserServiceUnitTest {

    private UserService userService;

    @BeforeEach
    void setup() {
        userService = new UserService();
    }

    @Test
    @DisplayName("createUser should persist phoneNumber correctly when valid phoneNumber is provided")
    void createUserShouldPersistValidPhoneNumber() {
        UserCreateRequest payload = new UserCreateRequest("Carlos", "carlos@example.com", "USER", "+55 11 91234-5678");

        UserResponse createdUser = userService.create(payload);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.phoneNumber()).isEqualTo("+55 11 91234-5678");
    }

    @Test
    @DisplayName("createUser should handle null phoneNumber by setting phoneNumber to null")
    void createUserShouldHandleNullPhoneNumber() {
        UserCreateRequest payload = new UserCreateRequest("Daniela", "daniela@example.com", "USER", null);

        UserResponse createdUser = userService.create(payload);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.phoneNumber()).isNull();
    }

    @Test
    @DisplayName("createUser should handle empty phoneNumber by setting phoneNumber to empty string")
    void createUserShouldHandleEmptyPhoneNumber() {
        UserCreateRequest payload = new UserCreateRequest("Eduardo", "eduardo@example.com", "USER", "");

        UserResponse createdUser = userService.create(payload);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.phoneNumber()).isEmpty();
    }

    @Nested
    @DisplayName("Parameterized tests for valid phoneNumber formats")
    class ValidPhoneNumberFormats {

        @ParameterizedTest(name = "Valid phoneNumber: {0}")
        @ValueSource(strings = {
                "+55 11 91234-5678",
                "+1 (555) 123-4567",
                "011 91234 5678",
                "912345678",
                "+44 20 7946 0958",
                "+55-11-91234-5678",
                "+55 (11) 91234 5678"
        })
        void createUserShouldAcceptValidPhoneNumbers(String phoneNumber) {
            UserCreateRequest payload = new UserCreateRequest("Test User", "testuser+" + phoneNumber.hashCode() + "@example.com", "USER", phoneNumber);

            UserResponse createdUser = userService.create(payload);

            assertThat(createdUser).isNotNull();
            assertThat(createdUser.phoneNumber()).isEqualTo(phoneNumber);
        }
    }

    @Nested
    @DisplayName("Parameterized tests for invalid phoneNumber formats")
    class InvalidPhoneNumberFormats {

        @ParameterizedTest(name = "Invalid phoneNumber: {0}")
        @ValueSource(strings = {
                "123",
                "abcde",
                "++55 11 91234-5678",
                "123-456-7890-1234-5678",
                "phone123!",
                "!!!@@@###",
                "     ",
                "\t\n"
        })
        void createUserShouldAcceptInvalidPhoneNumbersAsIs(String phoneNumber) {
            UserCreateRequest payload = new UserCreateRequest("Invalid User", "invaliduser+" + phoneNumber.hashCode() + "@example.com", "USER", phoneNumber);

            UserResponse createdUser = userService.create(payload);

            assertThat(createdUser).isNotNull();
            assertThat(createdUser.phoneNumber()).isEqualTo(phoneNumber);
        }
    }

    @Test
    @DisplayName("createUser should reject duplicate phoneNumber if rule exists (throws exception)")
    void createUserShouldRejectDuplicatePhoneNumberIfRuleExists() {
        // First create user with a phone number
        UserCreateRequest payload1 = new UserCreateRequest("User One", "userone@example.com", "USER", "+55 11 99999-9999");
        UserResponse created1 = userService.create(payload1);

        // Attempt to create another user with the same phone number
        UserCreateRequest payload2 = new UserCreateRequest("User Two", "usertwo@example.com", "USER", "+55 11 99999-9999");

        // Assuming UserService throws IllegalArgumentException on duplicate phoneNumber
        assertThatThrownBy(() -> userService.create(payload2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("phoneNumber already exists");
    }

    @Test
    @DisplayName("listAllUsers should return users with correct phoneNumber persisted")
    void listAllUsersShouldReturnUsersWithCorrectPhoneNumbers() {
        // Create users with different phone numbers
        UserCreateRequest payload1 = new UserCreateRequest("Alice", "alice@example.com", "USER", "+55 11 90000-0001");
        UserCreateRequest payload2 = new UserCreateRequest("Bob", "bob@example.com", "USER", "+55 11 90000-0002");

        UserResponse created1 = userService.create(payload1);
        UserResponse created2 = userService.create(payload2);

        List<UserResponse> users = userService.listAllUsers();

        assertThat(users).extracting(UserResponse::phoneNumber)
                .contains(created1.phoneNumber(), created2.phoneNumber());
    }

    @Test
    @DisplayName("UserCreateRequest serializes and deserializes correctly with various phoneNumber values")
    void userCreateRequestSerializationDeserialization() throws Exception {
        var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

        String[] phoneNumbers = {null, "", "+55 11 91234-5678", "invalid-phone-123!@#"};

        for (String phoneNumber : phoneNumbers) {
            UserCreateRequest original = new UserCreateRequest("Serialize Test", "serialize@example.com", "USER", phoneNumber);

            String json = objectMapper.writeValueAsString(original);
            UserCreateRequest deserialized = objectMapper.readValue(json, UserCreateRequest.class);

            assertThat(deserialized).isNotNull();
            assertThat(deserialized.name()).isEqualTo(original.name());
            assertThat(deserialized.email()).isEqualTo(original.email());
            assertThat(deserialized.role()).isEqualTo(original.role());
            assertThat(deserialized.phoneNumber()).isEqualTo(original.phoneNumber());
        }
    }

    @Test
    @DisplayName("UserCreateRequest deserialization fails with invalid JSON types for phoneNumber")
    void userCreateRequestDeserializationFailsWithInvalidPhoneNumberType() {
        var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

        String invalidJson = """
                {
                    "name": "Invalid Type",
                    "email": "invalidtype@example.com",
                    "role": "USER",
                    "phoneNumber": 12345
                }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(invalidJson, UserCreateRequest.class))
                .isInstanceOf(com.fasterxml.jackson.databind.exc.MismatchedInputException.class);
    }
}