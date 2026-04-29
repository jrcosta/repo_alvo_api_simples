package com.repoalvo.javaapi.controller;

import com.repoalvo.javaapi.service.ExternalService;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
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
    @DisplayName("deleteUser should delete existing user and return no content")
    void deleteUserShouldDeleteExistingUserAndReturnNoContent() {
        int userId = 10;

        when(userService.getById(userId)).thenReturn(java.util.Optional.of(mock(com.repoalvo.javaapi.model.UserResponse.class)));

        assertDoesNotThrow(() -> userController.deleteUser(userId));

        verify(userService, times(1)).getById(userId);
        verify(userService, times(1)).delete(userId);
    }

    @Test
    @DisplayName("deleteUser should throw 404 ResponseStatusException when user does not exist")
    void deleteUserShouldThrow404WhenUserDoesNotExist() {
        int userId = 20;

        when(userService.getById(userId)).thenReturn(java.util.Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userController.deleteUser(userId));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Usuário não encontrado", ex.getReason());

        verify(userService, times(1)).getById(userId);
        verify(userService, never()).delete(anyInt());
    }

    @Test
    @DisplayName("deleteUser should propagate unexpected exceptions from userService.delete")
    void deleteUserShouldPropagateUnexpectedExceptions() {
        int userId = 30;

        when(userService.getById(userId)).thenReturn(java.util.Optional.of(mock(com.repoalvo.javaapi.model.UserResponse.class)));
        doThrow(new RuntimeException("DB failure")).when(userService).delete(userId);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userController.deleteUser(userId));
        assertEquals("DB failure", ex.getMessage());

        verify(userService, times(1)).getById(userId);
        verify(userService, times(1)).delete(userId);
    }

    @Test
    @DisplayName("deleteUser should call userService.delete with correct userId")
    void deleteUserShouldCallUserServiceDeleteWithCorrectId() {
        int userId = 40;

        when(userService.getById(userId)).thenReturn(java.util.Optional.of(mock(com.repoalvo.javaapi.model.UserResponse.class)));

        userController.deleteUser(userId);

        verify(userService).delete(userId);
    }

    @Test
    @DisplayName("deleteUser should throw 404 ResponseStatusException for negative userId")
    void deleteUserShouldThrow404ForNegativeUserId() {
        int userId = -1;

        when(userService.getById(userId)).thenReturn(java.util.Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userController.deleteUser(userId));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Usuário não encontrado", ex.getReason());

        verify(userService, times(1)).getById(userId);
        verify(userService, never()).delete(anyInt());
    }

    @Test
    @DisplayName("deleteUser should throw 404 ResponseStatusException for zero userId")
    void deleteUserShouldThrow404ForZeroUserId() {
        int userId = 0;

        when(userService.getById(userId)).thenReturn(java.util.Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userController.deleteUser(userId));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Usuário não encontrado", ex.getReason());

        verify(userService, times(1)).getById(userId);
        verify(userService, never()).delete(anyInt());
    }
}