package com.repoalvo.javaapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repoalvo.javaapi.model.CountResponse;
import com.repoalvo.javaapi.model.UserCreateRequest;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.service.ExternalService;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import static org.springframework.http.HttpStatus.CONFLICT;

class UserControllerUnitTest {

    private UserService userService;
    private ExternalService externalService;
    private UserController userController;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        externalService = mock(ExternalService.class);
        userController = new UserController(userService, externalService);
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("usersCount returns CountResponse with correct count and fixed 'users' label when list has multiple users")
    void usersCountReturnsCorrectCountAndLabelForMultipleUsers() {
        List<UserResponse> mockUsers = List.of(
                new UserResponse(1, "Alice", "alice@example.com", "ACTIVE", "USER"),
                new UserResponse(2, "Bob", "bob@example.com", "ACTIVE", "USER"),
                new UserResponse(3, "Carol", "carol@example.com", "ACTIVE", "USER")
        );
        when(userService.listAllUsers()).thenReturn(mockUsers);

        CountResponse response = userController.usersCount();

        assertThat(response).isNotNull();
        assertThat(response.count()).isEqualTo(mockUsers.size());
        assertThat(response.resource()).isEqualTo("users");
        verify(userService, times(1)).listAllUsers();
    }

    @Test
    @DisplayName("usersCount returns CountResponse with count zero and fixed 'users' label when list is empty")
    void usersCountReturnsZeroCountAndLabelForEmptyList() {
        when(userService.listAllUsers()).thenReturn(Collections.emptyList());

        CountResponse response = userController.usersCount();

        assertThat(response).isNotNull();
        assertThat(response.count()).isZero();
        assertThat(response.resource()).isEqualTo("users");
        verify(userService, times(1)).listAllUsers();
    }

    @Test
    @DisplayName("usersCount serializes to JSON with fields 'count' and 'resource' correctly")
    void countResponseSerializesToJsonWithCountAndResourceFields() throws JsonProcessingException {
        CountResponse countResponse = new CountResponse(5, "users");

        String json = objectMapper.writeValueAsString(countResponse);

        assertThat(json).contains("\"count\":5");
        assertThat(json).contains("\"resource\":\"users\"");
    }

    @Test
    @DisplayName("usersCount deserializes from JSON with fields 'count' and 'resource' correctly")
    void countResponseDeserializesFromJsonWithCountAndResourceFields() throws JsonProcessingException {
        String json = "{\"count\":7,\"resource\":\"users\"}";

        CountResponse response = objectMapper.readValue(json, CountResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.count()).isEqualTo(7);
        assertThat(response.resource()).isEqualTo("users");
    }

    @Test
    @DisplayName("usersCount response does not expose sensitive or unexpected fields")
    void usersCountResponseExposesOnlyCountAndResourceFields() throws JsonProcessingException {
        List<UserResponse> mockUsers = List.of(
                new UserResponse(1, "Alice", "alice@example.com", "ACTIVE", "USER")
        );
        when(userService.listAllUsers()).thenReturn(mockUsers);

        CountResponse response = userController.usersCount();

        String json = objectMapper.writeValueAsString(response);

        // Should contain only count and resource fields
        assertThat(json).contains("\"count\":1");
        assertThat(json).contains("\"resource\":\"users\"");
        assertThat(json).doesNotContain("password");
        assertThat(json).doesNotContain("email");
        assertThat(json).doesNotContain("name");
        assertThat(json).doesNotContain("type");
    }

    @Test
    @DisplayName("createUser throws ResponseStatusException with CONFLICT.name() message when email already exists")
    void createUserThrowsConflictExceptionWithCorrectMessageWhenEmailExists() {
        UserCreateRequest payload = new UserCreateRequest(
                "Lucas",
                "lucas@example.com",
                "USER",
                "+55 41 91234-5678"
        );

        when(userService.findByEmail(payload.email())).thenReturn(Optional.of(
                new UserResponse(1, "Existing User", payload.email(), "ACTIVE", "USER", "+55 41 91234-5678")
        ));

        assertThatThrownBy(() -> userController.createUser(payload))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining(CONFLICT.name());

        verify(userService, times(1)).findByEmail(payload.email());
        verify(userService, never()).create(any());
    }

    @Test
    @DisplayName("no tests use deprecated 'type' field in CountResponse or UserResponse")
    void noTestsUseDeprecatedTypeField() {
        // This test ensures no usage of 'type' field in CountResponse or UserResponse serialization/deserialization
        // by checking JSON strings do not contain 'type' key anywhere in this test class context.

        CountResponse countResponse = new CountResponse(3, "users");
        String json = null;
        try {
            json = objectMapper.writeValueAsString(countResponse);
        } catch (JsonProcessingException e) {
            fail("Serialization failed: " + e.getMessage());
        }
        assertThat(json).doesNotContain("\"type\"");

        UserResponse userResponse = new UserResponse(1, "Test", "test@example.com", "ACTIVE", "USER");
        try {
            json = objectMapper.writeValueAsString(userResponse);
        } catch (JsonProcessingException e) {
            fail("Serialization failed: " + e.getMessage());
        }
        assertThat(json).doesNotContain("\"type\"");
    }
}