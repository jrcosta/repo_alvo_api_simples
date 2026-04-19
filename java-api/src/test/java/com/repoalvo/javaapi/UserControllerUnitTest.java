package com.repoalvo.javaapi;

import com.repoalvo.javaapi.controller.UserController;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.service.ExternalService;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;

class UserControllerUnitTest {

    private final UserService userService = mock(UserService.class);
    private final ExternalService externalService = mock(ExternalService.class);
    private final UserController userController = new UserController(userService, externalService);

    @Test
    @DisplayName("listUsers returns empty list when limit is zero and offset is negative")
    void listUsersShouldReturnEmptyListWhenLimitZeroAndOffsetNegative() {
        when(userService.listUsers(0, -1)).thenReturn(List.of());

        List<UserResponse> result = userController.listUsers(0, -1);

        assertThat(result).isEmpty();
        verify(userService, times(1)).listUsers(0, -1);
    }

    @Test
    @DisplayName("listUsers returns empty list when limit is very large and offset is negative")
    void listUsersShouldReturnEmptyListWhenLimitVeryLargeAndOffsetNegative() {
        int largeLimit = Integer.MAX_VALUE;
        when(userService.listUsers(largeLimit, -10)).thenReturn(List.of());

        List<UserResponse> result = userController.listUsers(largeLimit, -10);

        assertThat(result).isEmpty();
        verify(userService, times(1)).listUsers(largeLimit, -10);
    }

    @Test
    @DisplayName("firstUserEmail returns the first user when list has a single user")
    void firstUserEmailShouldReturnFirstUserWhenSingleUserExists() {
        UserResponse singleUser = new UserResponse(1, "Single User", "single@example.com");
        when(userService.listAllUsers()).thenReturn(List.of(singleUser));

        UserResponse result = userController.firstUserEmail();

        assertThat(result).isEqualTo(singleUser);
        verify(userService, times(1)).listAllUsers();
    }

    @Test
    @DisplayName("listUsers propagates exception thrown by userService")
    void listUsersShouldPropagateExceptionWhenUserServiceThrows() {
        when(userService.listUsers(5, 5)).thenThrow(new IllegalArgumentException("Invalid parameters"));

        assertThatThrownBy(() -> userController.listUsers(5, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid parameters");

        verify(userService, times(1)).listUsers(5, 5);
    }

}