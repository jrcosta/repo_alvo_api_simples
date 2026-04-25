package com.repoalvo.javaapi.controller;

import com.repoalvo.javaapi.model.UserCreateRequest;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.service.ExternalService;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import static org.springframework.http.HttpStatus.CONFLICT;

class UserControllerUnitTest {

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
    @DisplayName("createUser returns UserResponse with phoneNumber when provided in payload")
    void createUserShouldReturnUserResponseWithPhoneNumber() {
        UserCreateRequest payload = new UserCreateRequest(
                "Lucas",
                "lucas@example.com",
                "USER",
                "+55 41 91234-5678"
        );

        UserResponse expectedUser = new UserResponse(
                100,
                payload.name(),
                payload.email(),
                "ACTIVE",
                payload.role(),
                payload.phoneNumber()
        );

        when(userService.findByEmail(payload.email())).thenReturn(Optional.empty());
        when(userService.create(payload)).thenReturn(expectedUser);

        UserResponse response = userController.createUser(payload);

        assertThat(response).isNotNull();
        assertThat(response.phoneNumber()).isEqualTo(payload.phoneNumber());
        assertThat(response.name()).isEqualTo(payload.name());
        assertThat(response.email()).isEqualTo(payload.email());

        verify(userService, times(1)).findByEmail(payload.email());
        verify(userService, times(1)).create(payload);
    }

    @Test
    @DisplayName("createUser returns UserResponse with null phoneNumber when phoneNumber not provided")
    void createUserShouldReturnUserResponseWithNullPhoneNumberWhenNotProvided() {
        UserCreateRequest payload = new UserCreateRequest(
                "Lucas",
                "lucas@example.com",
                "USER",
                null
        );

        UserResponse expectedUser = new UserResponse(
                101,
                payload.name(),
                payload.email(),
                "ACTIVE",
                payload.role(),
                null
        );

        when(userService.findByEmail(payload.email())).thenReturn(Optional.empty());
        when(userService.create(payload)).thenReturn(expectedUser);

        UserResponse response = userController.createUser(payload);

        assertThat(response).isNotNull();
        assertThat(response.phoneNumber()).isNull();
        assertThat(response.name()).isEqualTo(payload.name());
        assertThat(response.email()).isEqualTo(payload.email());

        verify(userService, times(1)).findByEmail(payload.email());
        verify(userService, times(1)).create(payload);
    }

    @Test
    @DisplayName("createUser throws 409 Conflict when email already exists")
    void createUserShouldThrowConflictWhenEmailExists() {
        UserCreateRequest payload = new UserCreateRequest(
                "Lucas",
                "lucas@example.com",
                "USER",
                "+55 41 91234-5678"
        );

        when(userService.findByEmail(payload.email())).thenReturn(Optional.of(
                new UserResponse(1, "Existing User", payload.email(), "ACTIVE", "USER", "+55 41 91234-5678")
        ));

        assertThatThrownBy(() -> userController.createUser(payload))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining(CONFLICT.getReasonPhrase());

        verify(userService, times(1)).findByEmail(payload.email());
        verify(userService, never()).create(any());
    }
}