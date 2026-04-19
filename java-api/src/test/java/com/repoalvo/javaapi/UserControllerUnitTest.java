package com.repoalvo.javaapi;

import com.repoalvo.javaapi.controller.UserController;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.service.ExternalService;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @DisplayName("listUserNames delegates to userService.listAllUsers exactly once")
    void listUserNamesShouldCallListAllUsersOnce() {
        when(userService.listAllUsers()).thenReturn(List.of());

        userController.listUserNames();

        verify(userService, times(1)).listAllUsers();
    }

    @Test
    @DisplayName("listUserNames returns names sorted case-insensitively")
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
    @DisplayName("listUserNames returns empty list when there are no users")
    void listUserNamesShouldReturnEmptyListWhenNoUsers() {
        when(userService.listAllUsers()).thenReturn(List.of());

        List<String> result = userController.listUserNames();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("listUserNames preserves users with duplicate names")
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
    @DisplayName("listUserNames returns empty string as a name without throwing")
    void listUserNamesShouldHandleEmptyNameWithoutException() {
        List<UserResponse> users = List.of(
                new UserResponse(1, "", "emptyname@example.com"),
                new UserResponse(2, "Bruno", "bruno@example.com")
        );
        when(userService.listAllUsers()).thenReturn(users);

        List<String> result = userController.listUserNames();

        assertThat(result).containsExactlyInAnyOrder("", "Bruno");
    }

    @Test
    @DisplayName("listUserNames throws NullPointerException when a user has a null name")
    void listUserNamesShouldThrowWhenUserNameIsNull() {
        List<UserResponse> users = List.of(
                new UserResponse(1, null, "nullname@example.com"),
                new UserResponse(2, "Bruno", "bruno@example.com")
        );
        when(userService.listAllUsers()).thenReturn(users);

        // String::compareToIgnoreCase used as a comparator calls null.compareToIgnoreCase(...)
        // which throws NullPointerException; this documents the current behaviour.
        assertThatThrownBy(() -> userController.listUserNames())
                .isInstanceOf(NullPointerException.class);
    }
}