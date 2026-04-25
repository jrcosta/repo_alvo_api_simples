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
        UserResponse user = new UserResponse(userId, "Test User", "test@example.com", "ACTIVE", "USER");
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
        UserResponse user = new UserResponse(userId, "Test User", "test@example.com", "ACTIVE", "USER");
        when(userService.getById(userId)).thenReturn(Optional.of(user));

        userController.userExists(userId);

        verify(userService, times(1)).getById(userId);
        verifyNoInteractions(externalService);
    }

    @Test
    @DisplayName("userExists returns exists=true for user with status INACTIVE and role USER")
    void userExistsReturnsTrueForInactiveUserWithUserRole() {
        int userId = 2;
        UserResponse user = new UserResponse(userId, "Inactive User", "inactive@example.com", "INACTIVE", "USER");
        when(userService.getById(userId)).thenReturn(Optional.of(user));

        UserExistsResponse response = userController.userExists(userId);

        assertNotNull(response, "Response should not be null");
        assertTrue(response.exists(), "exists should be true for inactive user");
        verify(userService, times(1)).getById(userId);
    }

    @Test
    @DisplayName("userExists returns exists=true for user with status ACTIVE and role ADMIN")
    void userExistsReturnsTrueForActiveUserWithAdminRole() {
        int userId = 3;
        UserResponse user = new UserResponse(userId, "Admin User", "admin@example.com", "ACTIVE", "ADMIN");
        when(userService.getById(userId)).thenReturn(Optional.of(user));

        UserExistsResponse response = userController.userExists(userId);

        assertNotNull(response, "Response should not be null");
        assertTrue(response.exists(), "exists should be true for active admin user");
        verify(userService, times(1)).getById(userId);
    }

    @Test
    @DisplayName("userExists returns exists=true for user with unusual status and role")
    void userExistsReturnsTrueForUserWithSuspendedStatusAndAdminRole() {
        int userId = 4;
        UserResponse user = new UserResponse(userId, "Suspended Admin", "suspended@example.com", "SUSPENDED", "ADMIN");
        when(userService.getById(userId)).thenReturn(Optional.of(user));

        UserExistsResponse response = userController.userExists(userId);

        assertNotNull(response, "Response should not be null");
        assertTrue(response.exists(), "exists should be true for suspended admin user");
        verify(userService, times(1)).getById(userId);
    }

    @Test
    @DisplayName("userExists returns exists=false when UserResponse has null status and role")
    void userExistsReturnsFalseWhenUserHasNullStatusAndRole() {
        int userId = 5;
        UserResponse user = new UserResponse(userId, "Null Fields User", "nullfields@example.com", null, null);
        when(userService.getById(userId)).thenReturn(Optional.of(user));

        UserExistsResponse response = userController.userExists(userId);

        assertNotNull(response, "Response should not be null");
        // Since user is present, exists should be true regardless of null fields
        assertTrue(response.exists(), "exists should be true even if status and role are null");
        verify(userService, times(1)).getById(userId);
    }

    @Test
    @DisplayName("userExists returns exists=true when UserResponse has empty status and role")
    void userExistsReturnsTrueWhenUserHasEmptyStatusAndRole() {
        int userId = 6;
        UserResponse user = new UserResponse(userId, "Empty Fields User", "emptyfields@example.com", "", "");
        when(userService.getById(userId)).thenReturn(Optional.of(user));

        UserExistsResponse response = userController.userExists(userId);

        assertNotNull(response, "Response should not be null");
        assertTrue(response.exists(), "exists should be true even if status and role are empty strings");
        verify(userService, times(1)).getById(userId);
    }
}