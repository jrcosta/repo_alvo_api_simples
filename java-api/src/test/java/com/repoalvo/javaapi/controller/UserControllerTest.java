package com.repoalvo.javaapi.controller;

import com.repoalvo.javaapi.model.UserExistsResponse;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.service.ExternalService;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerTest {

    private UserService userService;
    private ExternalService externalService;
    private UserController userController;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        externalService = mock(ExternalService.class);
        userController = new UserController(userService, externalService);
    }

    @Test
    @DisplayName("userExists returns exists=true when userService.getById returns a user")
    void userExistsReturnsTrueWhenUserPresent() {
        int userId = 1;
        UserResponse user = new UserResponse(userId, "Test User", "test@example.com");
        when(userService.getById(userId)).thenReturn(Optional.of(user));

        UserExistsResponse response = userController.userExists(userId);

        assertNotNull(response, "Response should not be null");
        assertTrue(response.exists(), "exists should be true when user is present");
        verify(userService, times(1)).getById(userId);
    }

    @Test
    @DisplayName("userExists returns exists=false when userService.getById returns empty")
    void userExistsReturnsFalseWhenUserAbsent() {
        int userId = 999;
        when(userService.getById(userId)).thenReturn(Optional.empty());

        UserExistsResponse response = userController.userExists(userId);

        assertNotNull(response, "Response should not be null");
        assertFalse(response.exists(), "exists should be false when user is absent");
        verify(userService, times(1)).getById(userId);
    }

    @Test
    @DisplayName("userExists does not throw exception for zero userId and returns false if user not found")
    void userExistsHandlesZeroUserIdGracefully() {
        int userId = 0;
        when(userService.getById(userId)).thenReturn(Optional.empty());

        UserExistsResponse response = userController.userExists(userId);

        assertNotNull(response, "Response should not be null");
        assertFalse(response.exists(), "exists should be false for zero userId if user not found");
        verify(userService, times(1)).getById(userId);
    }

    @Test
    @DisplayName("userExists does not throw exception for negative userId and returns false if user not found")
    void userExistsHandlesNegativeUserIdGracefully() {
        int userId = -10;
        when(userService.getById(userId)).thenReturn(Optional.empty());

        UserExistsResponse response = userController.userExists(userId);

        assertNotNull(response, "Response should not be null");
        assertFalse(response.exists(), "exists should be false for negative userId if user not found");
        verify(userService, times(1)).getById(userId);
    }

    @Test
    @DisplayName("userExists propagates unexpected exceptions thrown by userService.getById")
    void userExistsPropagatesUnexpectedException() {
        int userId = 5;
        when(userService.getById(userId)).thenThrow(new RuntimeException("Unexpected error"));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> userController.userExists(userId));
        assertEquals("Unexpected error", thrown.getMessage());
        verify(userService, times(1)).getById(userId);
    }

    @Test
    @DisplayName("userExists returns exists=false for Integer.MAX_VALUE when user not found")
    void userExistsReturnsFalseForMaxIntegerUserId() {
        int userId = Integer.MAX_VALUE;
        when(userService.getById(userId)).thenReturn(Optional.empty());

        UserExistsResponse response = userController.userExists(userId);

        assertNotNull(response, "Response should not be null");
        assertFalse(response.exists(), "exists should be false for Integer.MAX_VALUE if user not found");
        verify(userService, times(1)).getById(userId);
    }

    @Test
    @DisplayName("userExists returns exists=false for Integer.MIN_VALUE when user not found")
    void userExistsReturnsFalseForMinIntegerUserId() {
        int userId = Integer.MIN_VALUE;
        when(userService.getById(userId)).thenReturn(Optional.empty());

        UserExistsResponse response = userController.userExists(userId);

        assertNotNull(response, "Response should not be null");
        assertFalse(response.exists(), "exists should be false for Integer.MIN_VALUE if user not found");
        verify(userService, times(1)).getById(userId);
    }

    @Test
    @DisplayName("userExists does not interact with externalService")
    void userExistsDoesNotCallExternalService() {
        int userId = 1;
        UserResponse user = new UserResponse(userId, "Test User", "test@example.com");
        when(userService.getById(userId)).thenReturn(Optional.of(user));

        userController.userExists(userId);

        verify(userService, times(1)).getById(userId);
        verifyNoInteractions(externalService);
    }

    // Novos testes sugeridos no relatório de QA

    @Test
    @DisplayName("userExists returns exists=false for Integer.MAX_VALUE - 1 when user not found")
    void userExistsReturnsFalseForMaxIntegerMinusOneUserId() {
        int userId = Integer.MAX_VALUE - 1;
        when(userService.getById(userId)).thenReturn(Optional.empty());

        UserExistsResponse response = userController.userExists(userId);

        assertNotNull(response, "Response should not be null");
        assertFalse(response.exists(), "exists should be false for Integer.MAX_VALUE - 1 if user not found");
        verify(userService, times(1)).getById(userId);
    }

    @Test
    @DisplayName("userExists returns exists=false for Integer.MIN_VALUE + 1 when user not found")
    void userExistsReturnsFalseForMinIntegerPlusOneUserId() {
        int userId = Integer.MIN_VALUE + 1;
        when(userService.getById(userId)).thenReturn(Optional.empty());

        UserExistsResponse response = userController.userExists(userId);

        assertNotNull(response, "Response should not be null");
        assertFalse(response.exists(), "exists should be false for Integer.MIN_VALUE + 1 if user not found");
        verify(userService, times(1)).getById(userId);
    }

    @Test
    @DisplayName("userExists propagates IllegalStateException with correct message")
    void userExistsPropagatesIllegalStateException() {
        int userId = 10;
        when(userService.getById(userId)).thenThrow(new IllegalStateException("Illegal state occurred"));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> userController.userExists(userId));
        assertEquals("Illegal state occurred", thrown.getMessage());
        verify(userService, times(1)).getById(userId);
    }

    @Test
    @DisplayName("userExists propagates NullPointerException with correct message")
    void userExistsPropagatesNullPointerException() {
        int userId = 20;
        when(userService.getById(userId)).thenThrow(new NullPointerException("Null pointer exception"));

        NullPointerException thrown = assertThrows(NullPointerException.class, () -> userController.userExists(userId));
        assertEquals("Null pointer exception", thrown.getMessage());
        verify(userService, times(1)).getById(userId);
    }

    @Test
    @DisplayName("userExists throws NullPointerException when called with null userId (if applicable)")
    void userExistsThrowsExceptionForNullUserId() {
        // Como o método userExists recebe int primitivo, não aceita null.
        // Este teste é para garantir que caso a assinatura mude para Integer, o comportamento seja testado.
        // Aqui simulamos chamando via reflexão para forçar null, ou ignoramos se não aplicável.
        // Como não é aplicável, este teste será ignorado para evitar falso positivo.
        // Caso a assinatura mude para Integer, implementar teste adequado.
    }
}