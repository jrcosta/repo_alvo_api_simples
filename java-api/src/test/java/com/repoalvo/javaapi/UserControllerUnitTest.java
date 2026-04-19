package com.repoalvo.javaapi;

import com.repoalvo.javaapi.controller.UserController;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.service.ExternalService;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
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

    // Removed tests for listUserNames() as method no longer exists

    @Test
    @DisplayName("UserController should not have method listUserNames")
    void userControllerShouldNotHaveListUserNamesMethod() {
        Method[] methods = UserController.class.getDeclaredMethods();
        boolean hasListUserNames = false;
        for (Method method : methods) {
            if ("listUserNames".equals(method.getName())) {
                hasListUserNames = true;
                break;
            }
        }
        assertThat(hasListUserNames).isFalse();
    }

    @Test
    @DisplayName("listUsers returns list from userService with given limit and offset")
    void listUsersShouldReturnUsersFromService() {
        List<UserResponse> users = List.of(
                new UserResponse(1, "Ana Silva", "ana@example.com"),
                new UserResponse(2, "Bruno Lima", "bruno@example.com")
        );
        when(userService.listUsers(10, 5)).thenReturn(users);

        List<UserResponse> result = userController.listUsers(10, 5);

        assertThat(result).isEqualTo(users);
        verify(userService, times(1)).listUsers(10, 5);
    }

    @Test
    @DisplayName("usersCount returns count of all users from userService")
    void usersCountShouldReturnCorrectCount() {
        List<UserResponse> allUsers = List.of(
                new UserResponse(1, "Ana Silva", "ana@example.com"),
                new UserResponse(2, "Bruno Lima", "bruno@example.com"),
                new UserResponse(3, "Carlos", "carlos@example.com")
        );
        when(userService.listAllUsers()).thenReturn(allUsers);

        var countResponse = userController.usersCount();

        assertThat(countResponse).isNotNull();
        assertThat(countResponse.count()).isEqualTo(allUsers.size());
        verify(userService, times(1)).listAllUsers();
    }

    @Test
    @DisplayName("findDuplicateUsers returns duplicates based on email")
    void findDuplicateUsersShouldReturnUsersWithDuplicateEmails() {
        UserResponse user1 = new UserResponse(1, "Ana Silva", "ana@example.com");
        UserResponse user2 = new UserResponse(2, "Bruno Lima", "bruno@example.com");
        UserResponse user3 = new UserResponse(3, "Ana Silva Duplicate", "ana@example.com");
        List<UserResponse> allUsers = List.of(user1, user2, user3);
        when(userService.listAllUsers()).thenReturn(allUsers);

        List<UserResponse> duplicates = userController.findDuplicateUsers();

        assertThat(duplicates).containsExactlyInAnyOrder(user1, user3);
        verify(userService, times(1)).listAllUsers();
    }

    @Test
    @DisplayName("searchUsers returns users whose names contain search term case-insensitively")
    void searchUsersShouldFilterUsersByName() {
        UserResponse user1 = new UserResponse(1, "Ana Silva", "ana@example.com");
        UserResponse user2 = new UserResponse(2, "Bruno Lima", "bruno@example.com");
        UserResponse user3 = new UserResponse(3, "Carlos", "carlos@example.com");
        List<UserResponse> allUsers = List.of(user1, user2, user3);
        when(userService.listAllUsers()).thenReturn(allUsers);

        List<UserResponse> result = userController.searchUsers("an");

        assertThat(result).containsExactly(user1);
        verify(userService, times(1)).listAllUsers();
    }
}