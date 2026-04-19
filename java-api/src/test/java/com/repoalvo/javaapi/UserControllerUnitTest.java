package com.repoalvo.javaapi;

import com.repoalvo.javaapi.controller.UserController;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.service.ExternalService;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class UserControllerUnitTest {

    private UserService userService;
    private ExternalService externalService;
    private UserController userController;

    @BeforeEach
    void setup() {
        userService = mock(UserService.class);
        externalService = mock(ExternalService.class);
        userController = new UserController(userService, externalService);
    }

    @Test
    void listUserNamesShouldCallListAllUsersOnce() {
        when(userService.listAllUsers()).thenReturn(List.of());

        userController.listUserNames();

        verify(userService, times(1)).listAllUsers();
    }

    @Test
    void listUserNamesShouldReturnSortedNamesIgnoringCase() {
        List<UserResponse> users = List.of(
                new UserResponse(1, "Bruno", "bruno@example.com"),
                new UserResponse(2, "ana", "ana@example.com"),
                new UserResponse(3, "Carlos", "carlos@example.com")
        );
        when(userService.listAllUsers()).thenReturn(users);

        List<String> result = userController.listUserNames();

        assertThat(result).containsExactly("ana", "Bruno", "Carlos");
    }

    @Test
    void listUserNamesShouldReturnEmptyListWhenNoUsers() {
        when(userService.listAllUsers()).thenReturn(List.of());

        List<String> result = userController.listUserNames();

        assertThat(result).isEmpty();
    }

    @Test
    void listUserNamesShouldIncludeDuplicateNames() {
        List<UserResponse> users = List.of(
                new UserResponse(1, "Ana", "ana1@example.com"),
                new UserResponse(2, "ana", "ana2@example.com"),
                new UserResponse(3, "Bruno", "bruno@example.com")
        );
        when(userService.listAllUsers()).thenReturn(users);

        List<String> result = userController.listUserNames();

        assertThat(result).containsExactly("Ana", "ana", "Bruno");
    }

    @Test
    void listUserNamesShouldHandleNullAndEmptyNamesWithoutException() {
        List<UserResponse> users = List.of(
                new UserResponse(1, null, "nullname@example.com"),
                new UserResponse(2, "", "emptyname@example.com"),
                new UserResponse(3, "Bruno", "bruno@example.com")
        );
        when(userService.listAllUsers()).thenReturn(users);

        // The current implementation does not explicitly handle null names,
        // so this test verifies it does not throw NullPointerException.
        // It may throw NPE if sorting encounters null, so we catch and fail if that happens.
        assertDoesNotThrow(() -> {
            List<String> result = userController.listUserNames();
            // The result should contain null and empty string as is, sorted ignoring case.
            // Sorting with null will throw NPE, so if no exception, nulls are handled.
            // If nulls are present, they should be first or last depending on sort.
            assertThat(result).containsExactlyInAnyOrder(null, "", "Bruno");
        });
    }
}