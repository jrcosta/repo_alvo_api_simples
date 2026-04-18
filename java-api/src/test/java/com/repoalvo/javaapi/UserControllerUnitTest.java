package com.repoalvo.javaapi;

import com.repoalvo.javaapi.controller.UserController;
import com.repoalvo.javaapi.model.UserExistsResponse;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserControllerUnitTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void userExistsShouldReturnTrueWhenUserExists() {
        int userId = 1;
        when(userService.getById(userId)).thenReturn(Optional.of(mock(Object.class)));

        ResponseEntity<UserExistsResponse> response = userController.userExists(userId);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isExists()).isTrue();

        verify(userService, times(1)).getById(userId);
    }

    @Test
    void userExistsShouldReturnFalseWhenUserDoesNotExist() {
        int userId = 999;
        when(userService.getById(userId)).thenReturn(Optional.empty());

        ResponseEntity<UserExistsResponse> response = userController.userExists(userId);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isExists()).isFalse();

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
    void userExistsShouldHandleInvalidIdGracefully() {
        // Since the controller method receives int userId, invalid strings are rejected by Spring before reaching controller.
        // But negative or zero IDs are accepted as int, so test behavior with negative ID.
        int invalidUserId = -1;
        when(userService.getById(invalidUserId)).thenReturn(Optional.empty());

        ResponseEntity<UserExistsResponse> response = userController.userExists(invalidUserId);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isExists()).isFalse();

        verify(userService, times(1)).getById(invalidUserId);
    }
}