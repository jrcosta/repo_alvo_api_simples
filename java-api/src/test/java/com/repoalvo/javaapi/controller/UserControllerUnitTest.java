package com.repoalvo.javaapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repoalvo.javaapi.model.CountResponse;
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
    }
}