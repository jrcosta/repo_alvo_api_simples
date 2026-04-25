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
        assertThat(response.type()).isEqualTo("users");
        verify(userService, times(1)).listAllUsers();
    }

    @Test
    @DisplayName("usersCount returns CountResponse with count zero and fixed 'users' label when list is empty")
    void usersCountReturnsZeroCountAndLabelForEmptyList() {
        when(userService.listAllUsers()).thenReturn(Collections.emptyList());

        CountResponse response = userController.usersCount();

        assertThat(response).isNotNull();
        assertThat(response.count()).isZero();
        assertThat(response.type()).isEqualTo("users");
        verify(userService, times(1)).listAllUsers();
    }

    @Test
    @DisplayName("usersCount serializes to JSON with fields 'count' and 'users' correctly")
    void countResponseSerializesToJsonWithCountAndUsersFields() throws JsonProcessingException {
        CountResponse countResponse = new CountResponse(5, "users");

        String json = objectMapper.writeValueAsString(countResponse);

        assertThat(json).contains("\"count\":5");
        assertThat(json).contains("\"users\":\"users\"");
    }

    @Test
    @DisplayName("usersCount deserializes from JSON with fields 'count' and 'users' correctly")
    void countResponseDeserializesFromJsonWithCountAndUsersFields() throws JsonProcessingException {
        String json = "{\"count\":7,\"users\":\"users\"}";

        CountResponse response = objectMapper.readValue(json, CountResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.count()).isEqualTo(7);
        assertThat(response.type()).isEqualTo("users");
    }

    @Test
    @DisplayName("usersCount returns CountResponse with correct count and label for large list")
    void usersCountHandlesLargeUserListCorrectly() {
        int largeCount = 1000;
        List<UserResponse> largeList = Collections.nCopies(largeCount,
                new UserResponse(1, "User", "user@example.com", "ACTIVE", "USER"));
        when(userService.listAllUsers()).thenReturn(largeList);

        CountResponse response = userController.usersCount();

        assertThat(response).isNotNull();
        assertThat(response.count()).isEqualTo(largeCount);
        assertThat(response.type()).isEqualTo("users");
        verify(userService, times(1)).listAllUsers();
    }

    @Test
    @DisplayName("usersCount throws ResponseStatusException when userService.listAllUsers throws exception")
    void usersCountHandlesExceptionFromUserService() {
        when(userService.listAllUsers()).thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> userController.usersCount())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");

        verify(userService, times(1)).listAllUsers();
    }

    @Test
    @DisplayName("usersCount response does not expose sensitive or unexpected fields")
    void usersCountResponseExposesOnlyCountAndUsersFields() throws JsonProcessingException {
        List<UserResponse> mockUsers = List.of(
                new UserResponse(1, "Alice", "alice@example.com", "ACTIVE", "USER")
        );
        when(userService.listAllUsers()).thenReturn(mockUsers);

        CountResponse response = userController.usersCount();

        String json = objectMapper.writeValueAsString(response);

        // Should contain only count and users fields
        assertThat(json).contains("\"count\":1");
        assertThat(json).contains("\"users\":\"users\"");
        assertThat(json).doesNotContain("password");
        assertThat(json).doesNotContain("email");
        assertThat(json).doesNotContain("name");
    }

    @Test
    @DisplayName("usersCount response field 'users' is never null or empty")
    void usersCountResponseUsersFieldIsNeverNullOrEmpty() {
        List<UserResponse> mockUsers = List.of(
                new UserResponse(1, "Alice", "alice@example.com", "ACTIVE", "USER")
        );
        when(userService.listAllUsers()).thenReturn(mockUsers);

        CountResponse response = userController.usersCount();

        assertThat(response.type()).isNotNull().isNotEmpty();
        assertThat(response.type()).isEqualTo("users");
    @DisplayName("createUser returns UserResponse with phoneNumber when provided in payload")
    void createUserShouldReturnUserResponseWithPhoneNumber() {
        UserCreateRequest payload = new UserCreateRequest(
                "Lucas",
                "lucas@example.com",
                "USER",
                "+55 41 91234-5678"
        );

        UserResponse expectedUser = new UserResponse(
                100,
                payload.name(),
                payload.email(),
                "ACTIVE",
                payload.role(),
                payload.phoneNumber()
        );

        when(userService.findByEmail(payload.email())).thenReturn(Optional.empty());
        when(userService.create(payload)).thenReturn(expectedUser);

        UserResponse response = userController.createUser(payload);

        assertThat(response).isNotNull();
        assertThat(response.phoneNumber()).isEqualTo(payload.phoneNumber());
        assertThat(response.name()).isEqualTo(payload.name());
        assertThat(response.email()).isEqualTo(payload.email());

        verify(userService, times(1)).findByEmail(payload.email());
        verify(userService, times(1)).create(payload);
    }

    @Test
    @DisplayName("createUser returns UserResponse with null phoneNumber when phoneNumber not provided")
    void createUserShouldReturnUserResponseWithNullPhoneNumberWhenNotProvided() {
        UserCreateRequest payload = new UserCreateRequest(
                "Lucas",
                "lucas@example.com",
                "USER",
                null
        );

        UserResponse expectedUser = new UserResponse(
                101,
                payload.name(),
                payload.email(),
                "ACTIVE",
                payload.role(),
                null
        );

        when(userService.findByEmail(payload.email())).thenReturn(Optional.empty());
        when(userService.create(payload)).thenReturn(expectedUser);

        UserResponse response = userController.createUser(payload);

        assertThat(response).isNotNull();
        assertThat(response.phoneNumber()).isNull();
        assertThat(response.name()).isEqualTo(payload.name());
        assertThat(response.email()).isEqualTo(payload.email());

        verify(userService, times(1)).findByEmail(payload.email());
        verify(userService, times(1)).create(payload);
    }

    @Test
    @DisplayName("createUser throws 409 Conflict when email already exists")
    void createUserShouldThrowConflictWhenEmailExists() {
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
                .hasMessageContaining(CONFLICT.getReasonPhrase());

        verify(userService, times(1)).findByEmail(payload.email());
        verify(userService, never()).create(any());
    }
}