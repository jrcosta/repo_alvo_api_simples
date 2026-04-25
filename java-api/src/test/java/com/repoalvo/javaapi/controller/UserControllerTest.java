package com.repoalvo.javaapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        externalService = mock(ExternalService.class);
        userController = new UserController(userService, externalService);
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("UserExistsResponse constructor with exists and userId sets fields correctly")
    void userExistsResponseConstructorSetsFieldsCorrectly() {
        boolean exists = true;
        int userId = 42;

        UserExistsResponse response = new UserExistsResponse(exists, userId);

        assertNotNull(response, "Response object should not be null");
        assertEquals(exists, response.exists(), "exists field should match constructor argument");
        assertEquals(userId, response.userId(), "userId field should match constructor argument");
    }

    @Test
    @DisplayName("userExists returns exists=true and correct userId when userService returns user")
    void userExistsReturnsTrueAndUserIdWhenUserPresent() {
        int userId = 1;
        UserResponse user = new UserResponse(userId, "Test User", "test@example.com", "ACTIVE", "USER");
        when(userService.getById(userId)).thenReturn(Optional.of(user));

        UserExistsResponse response = userController.userExists(userId);

        assertNotNull(response, "Response should not be null");
        assertTrue(response.exists(), "exists should be true when user is present");
        assertEquals(userId, response.userId(), "userId should match the requested userId");
        verify(userService, times(1)).getById(userId);
    }

    @Test
    @DisplayName("userExists returns exists=false and correct userId when userService returns empty")
    void userExistsReturnsFalseAndUserIdWhenUserAbsent() {
        int userId = 9999;
        when(userService.getById(userId)).thenReturn(Optional.empty());

        UserExistsResponse response = userController.userExists(userId);

        assertNotNull(response, "Response should not be null");
        assertFalse(response.exists(), "exists should be false when user is absent");
        assertEquals(userId, response.userId(), "userId should match the requested userId");
        verify(userService, times(1)).getById(userId);
    }

    @Test
    @DisplayName("userExists does not throw exception and returns exists=false for zero userId")
    void userExistsHandlesZeroUserIdGracefully() {
        int userId = 0;
        when(userService.getById(userId)).thenReturn(Optional.empty());

        UserExistsResponse response = userController.userExists(userId);

        assertNotNull(response, "Response should not be null");
        assertFalse(response.exists(), "exists should be false for zero userId if user not found");
        assertEquals(userId, response.userId(), "userId should match the requested userId");
        verify(userService, times(1)).getById(userId);
    }

    @Test
    @DisplayName("userExists does not throw exception and returns exists=false for negative userId")
    void userExistsHandlesNegativeUserIdGracefully() {
        int userId = -1;
        when(userService.getById(userId)).thenReturn(Optional.empty());

        UserExistsResponse response = userController.userExists(userId);

        assertNotNull(response, "Response should not be null");
        assertFalse(response.exists(), "exists should be false for negative userId if user not found");
        assertEquals(userId, response.userId(), "userId should match the requested userId");
        verify(userService, times(1)).getById(userId);
    }

    @Test
    @DisplayName("userExists returns exists=false and correct userId for very large userId")
    void userExistsHandlesVeryLargeUserIdGracefully() {
        int userId = Integer.MAX_VALUE;
        when(userService.getById(userId)).thenReturn(Optional.empty());

        UserExistsResponse response = userController.userExists(userId);

        assertNotNull(response, "Response should not be null");
        assertFalse(response.exists(), "exists should be false for very large userId if user not found");
        assertEquals(userId, response.userId(), "userId should match the requested userId");
        verify(userService, times(1)).getById(userId);
    }

    @Test
    @DisplayName("UserExistsResponse serializes to JSON including both exists and userId fields")
    void userExistsResponseSerializesToJsonWithBothFields() throws JsonProcessingException {
        boolean exists = true;
        int userId = 123;
        UserExistsResponse response = new UserExistsResponse(exists, userId);

        String json = objectMapper.writeValueAsString(response);

        assertTrue(json.contains("\"exists\":" + exists), "JSON should contain exists field with correct value");
        assertTrue(json.contains("\"userId\":" + userId), "JSON should contain userId field with correct value");
    }
}