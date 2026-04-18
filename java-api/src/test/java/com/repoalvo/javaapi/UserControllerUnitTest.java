package com.repoalvo.javaapi;

import com.repoalvo.javaapi.controller.UserController;
import com.repoalvo.javaapi.model.UserExistsResponse;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.service.ExternalService;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserControllerUnitTest {

    @Mock
    private UserService userService;

    @Mock
    private ExternalService externalService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void userExistsShouldReturnTrueWhenUserExists() {
        int userId = 1;
        UserResponse user = new UserResponse(userId, "Ana Silva", "ana@example.com");
        when(userService.getById(userId)).thenReturn(Optional.of(user));

        UserExistsResponse response = userController.userExists(userId);

        assertThat(response).isNotNull();
        assertThat(response.exists()).isTrue();

        verify(userService, times(1)).getById(userId);
    }

    @Test
    void userExistsShouldReturnFalseWhenUserDoesNotExist() {
        int userId = 999;
        when(userService.getById(userId)).thenReturn(Optional.empty());

        UserExistsResponse response = userController.userExists(userId);

        assertThat(response).isNotNull();
        assertThat(response.exists()).isFalse();

        verify(userService, times(1)).getById(userId);
    }

    @Test
    void userExistsShouldPropagateUnexpectedException() {
        int userId = 5;
        when(userService.getById(userId)).thenThrow(new RuntimeException("Unexpected error"));

        assertThatThrownBy(() -> userController.userExists(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unexpected error");

        verify(userService, times(1)).getById(userId);
    }

    @Test
    void userExistsShouldReturnFalseForNegativeId() {
        int invalidUserId = -1;
        when(userService.getById(invalidUserId)).thenReturn(Optional.empty());

        UserExistsResponse response = userController.userExists(invalidUserId);

        assertThat(response).isNotNull();
        assertThat(response.exists()).isFalse();

        verify(userService, times(1)).getById(invalidUserId);
    }
}