package com.repoalvo.javaapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.model.UserStatusSummaryResponse;
import com.repoalvo.javaapi.service.ExternalService;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
    @DisplayName("usersStatusSummary returns empty map when user list is empty")
    void usersStatusSummaryReturnsEmptyMapWhenNoUsers() {
        when(userService.listAllUsers()).thenReturn(List.of());

        UserStatusSummaryResponse response = userController.usersStatusSummary();

        assertThat(response).isNotNull();
        assertThat(response.statuses()).isNotNull();
        assertThat(response.statuses()).isEmpty();

        verify(userService, times(1)).listAllUsers();
    }

    @Test
    @DisplayName("usersStatusSummary returns correct counts for multiple user statuses")
    void usersStatusSummaryReturnsCorrectCountsForMultipleStatuses() {
        List<UserResponse> users = List.of(
                new UserResponse(1, "Alice", "alice@example.com", "ACTIVE", "USER"),
                new UserResponse(2, "Bob", "bob@example.com", "INACTIVE", "USER"),
                new UserResponse(3, "Carol", "carol@example.com", "ACTIVE", "USER"),
                new UserResponse(4, "Dave", "dave@example.com", "PENDING", "USER"),
                new UserResponse(5, "Eve", "eve@example.com", "INACTIVE", "USER")
        );
        when(userService.listAllUsers()).thenReturn(users);

        UserStatusSummaryResponse response = userController.usersStatusSummary();

        assertThat(response).isNotNull();
        Map<String, Long> statuses = response.statuses();
        assertThat(statuses).isNotNull();
        assertThat(statuses).hasSize(3);
        assertThat(statuses).containsEntry("ACTIVE", 2L);
        assertThat(statuses).containsEntry("INACTIVE", 2L);
        assertThat(statuses).containsEntry("PENDING", 1L);

        verify(userService, times(1)).listAllUsers();
    }

    @Test
    @DisplayName("usersStatusSummary handles users with null or empty status keys")
    void usersStatusSummaryHandlesNullOrEmptyStatus() {
        List<UserResponse> users = List.of(
                new UserResponse(1, "Alice", "alice@example.com", null, "USER"),
                new UserResponse(2, "Bob", "bob@example.com", "", "USER"),
                new UserResponse(3, "Carol", "carol@example.com", "ACTIVE", "USER"),
                new UserResponse(4, "Dave", "dave@example.com", null, "USER")
        );
        when(userService.listAllUsers()).thenReturn(users);

        UserStatusSummaryResponse response = userController.usersStatusSummary();

        assertThat(response).isNotNull();
        Map<String, Long> statuses = response.statuses();
        assertThat(statuses).isNotNull();

        // The groupingBy will create keys for null and empty string as is
        assertThat(statuses).containsEntry(null, 2L);
        assertThat(statuses).containsEntry("", 1L);
        assertThat(statuses).containsEntry("ACTIVE", 1L);

        verify(userService, times(1)).listAllUsers();
    }

    @Test
    @DisplayName("usersStatusSummary returns a UserStatusSummaryResponse with the expected map")
    void usersStatusSummaryCreatesResponseWithExpectedMap() {
        Map<String, Long> expectedMap = Map.of("ACTIVE", 3L, "INACTIVE", 1L);
        List<UserResponse> users = List.of(
                new UserResponse(1, "Alice", "alice@example.com", "ACTIVE", "USER"),
                new UserResponse(2, "Bob", "bob@example.com", "ACTIVE", "USER"),
                new UserResponse(3, "Carol", "carol@example.com", "ACTIVE", "USER"),
                new UserResponse(4, "Dave", "dave@example.com", "INACTIVE", "USER")
        );
        when(userService.listAllUsers()).thenReturn(users);

        UserStatusSummaryResponse response = userController.usersStatusSummary();

        assertThat(response).isNotNull();
        assertThat(response.statuses()).isEqualTo(expectedMap);

        verify(userService, times(1)).listAllUsers();
    }
}