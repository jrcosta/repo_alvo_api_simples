package com.repoalvo.javaapi;

import com.repoalvo.javaapi.controller.UserController;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.service.ExternalService;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

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
    @DisplayName("getUserByEmail returns UserResponse when email exists")
    void getUserByEmailShouldReturnUserResponseWhenEmailExists() {
        String email = "ana@example.com";
        UserResponse user = new UserResponse(1, "Ana Silva", email);
        when(userService.findByEmail(email)).thenReturn(Optional.of(user));

        UserResponse result = userController.getUserByEmail(email);

        assertThat(result).isEqualTo(user);
        verify(userService, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("getUserByEmail throws ResponseStatusException 404 when email does not exist")
    void getUserByEmailShouldThrowNotFoundWhenEmailDoesNotExist() {
        String email = "naoexiste@example.com";
        when(userService.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userController.getUserByEmail(email))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(NOT_FOUND);
                    assertThat(rse.getReason()).isEqualTo("Usuário não encontrado");
                });

        verify(userService, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("getUserByEmail calls userService.findByEmail exactly once with correct email")
    void getUserByEmailShouldCallUserServiceFindByEmailOnce() {
        String email = "bruno@example.com";
        UserResponse user = new UserResponse(2, "Bruno Lima", email);
        when(userService.findByEmail(email)).thenReturn(Optional.of(user));

        userController.getUserByEmail(email);

        verify(userService, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("getUserByEmail throws ResponseStatusException 404 when email is empty string")
    void getUserByEmailShouldThrowNotFoundWhenEmailIsEmpty() {
        String email = "";
        when(userService.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userController.getUserByEmail(email))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(NOT_FOUND);
                    assertThat(rse.getReason()).isEqualTo("Usuário não encontrado");
                });

        verify(userService, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("getUserByEmail throws NullPointerException when email is null")
    void getUserByEmailShouldThrowExceptionWhenEmailIsNull() {
        // The method parameter is annotated with @RequestParam String email,
        // so null is not expected in normal controller usage.
        // But testing method directly for robustness.
        assertThatThrownBy(() -> userController.getUserByEmail(null))
                .isInstanceOf(NullPointerException.class);

        verify(userService, never()).findByEmail(any());
    }
}