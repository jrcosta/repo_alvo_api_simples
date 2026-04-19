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

    @Test
    void userExistsShouldReturnFalseForZeroId() {
        int zeroUserId = 0;
        when(userService.getById(zeroUserId)).thenReturn(Optional.empty());

        UserExistsResponse response = userController.userExists(zeroUserId);

        assertThat(response).isNotNull();
        assertThat(response.exists()).isFalse();

        verify(userService, times(1)).getById(zeroUserId);
    }

    @Test
    void userExistsShouldNotCallExternalService() {
        int userId = 1;
        UserResponse user = new UserResponse(userId, "Ana Silva", "ana@example.com");
        when(userService.getById(userId)).thenReturn(Optional.of(user));

        userController.userExists(userId);

        verify(userService, times(1)).getById(userId);
        verifyNoInteractions(externalService);
    }

    @Test
    void userExistsShouldBeIdempotentAndNotChangeState() {
        int userId = 3;
        UserResponse user = new UserResponse(userId, "Carlos Silva", "carlos@example.com");
        when(userService.getById(userId)).thenReturn(Optional.of(user));

        UserExistsResponse firstResponse = userController.userExists(userId);
        UserExistsResponse secondResponse = userController.userExists(userId);

        assertThat(firstResponse).isNotNull();
        assertThat(secondResponse).isNotNull();
        assertThat(firstResponse.exists()).isTrue();
        assertThat(secondResponse.exists()).isTrue();

        verify(userService, times(2)).getById(userId);
        verifyNoMoreInteractions(userService);
        verifyNoInteractions(externalService);
    }

    @Test
    void userExistsResponseShouldContainOnlyExistsField() {
        int userId = 4;
        UserResponse user = new UserResponse(userId, "Maria Silva", "maria@example.com");
        when(userService.getById(userId)).thenReturn(Optional.of(user));

        UserExistsResponse response = userController.userExists(userId);

        assertThat(response).isNotNull();
        assertThat(response.exists()).isTrue();

        // Assuming UserExistsResponse only has 'exists' field, check no other fields via reflection
        // This is a simple check to ensure no unexpected fields are present
        var fields = response.getClass().getDeclaredFields();
        assertThat(fields).hasSize(1);
        assertThat(fields[0].getName()).isEqualTo("exists");

        verify(userService, times(1)).getById(userId);
        verifyNoInteractions(externalService);
    }
}