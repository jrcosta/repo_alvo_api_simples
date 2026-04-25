package com.repoalvo.javaapi.controller;

import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.model.UserUpdateRequest;
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

import static org.springframework.http.HttpStatus.*;

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
    @DisplayName("updateUser updates user with only name provided and returns updated user including status and role")
    void updateUserShouldUpdateNameOnlyAndReturnUserWithStatusAndRole() {
        int userId = 1;
        UserUpdateRequest payload = new UserUpdateRequest("New Name", null);
        UserResponse updatedUser = new UserResponse(userId, "New Name", "oldemail@example.com", "ACTIVE", "USER");

        when(userService.update(eq(userId), eq(payload))).thenReturn(Optional.of(updatedUser));

        UserResponse result = userController.updateUser(userId, payload);

        assertThat(result).isEqualTo(updatedUser);
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.role()).isEqualTo("USER");
        verify(userService, never()).findByEmail(any());
        verify(userService, times(1)).update(userId, payload);
    }

    @Test
    @DisplayName("updateUser updates user with only email provided and returns updated user including status and role")
    void updateUserShouldUpdateEmailOnlyAndReturnUserWithStatusAndRole() {
        int userId = 2;
        String newEmail = "newemail@example.com";
        UserUpdateRequest payload = new UserUpdateRequest(null, newEmail);
        UserResponse updatedUser = new UserResponse(userId, "Existing Name", newEmail, "ACTIVE", "USER");

        when(userService.findByEmail(newEmail)).thenReturn(Optional.empty());
        when(userService.update(eq(userId), eq(payload))).thenReturn(Optional.of(updatedUser));

        UserResponse result = userController.updateUser(userId, payload);

        assertThat(result).isEqualTo(updatedUser);
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.role()).isEqualTo("USER");
        verify(userService, times(1)).findByEmail(newEmail);
        verify(userService, times(1)).update(userId, payload);
    }

    @Test
    @DisplayName("updateUser throws 409 Conflict when email is already used by another user")
    void updateUserShouldThrowConflictWhenEmailUsedByAnotherUser() {
        int userId = 3;
        String conflictingEmail = "conflict@example.com";
        UserUpdateRequest payload = new UserUpdateRequest(null, conflictingEmail);
        UserResponse otherUser = new UserResponse(99, "Other User", conflictingEmail, "ACTIVE", "USER");

        when(userService.findByEmail(conflictingEmail)).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> userController.updateUser(userId, payload))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(CONFLICT);
                    assertThat(rse.getReason()).isEqualTo("E-mail já cadastrado por outro usuário");
                });

        verify(userService, times(1)).findByEmail(conflictingEmail);
        verify(userService, never()).update(anyInt(), any());
    }

    @Test
    @DisplayName("updateUser allows update when email belongs to same user and returns user with status and role")
    void updateUserShouldAllowUpdateWhenEmailBelongsToSameUser() {
        int userId = 4;
        String sameEmail = "sameuser@example.com";
        UserUpdateRequest payload = new UserUpdateRequest(null, sameEmail);
        UserResponse sameUser = new UserResponse(userId, "Same User", sameEmail, "ACTIVE", "USER");
        UserResponse updatedUser = new UserResponse(userId, "Same User Updated", sameEmail, "ACTIVE", "USER");

        when(userService.findByEmail(sameEmail)).thenReturn(Optional.of(sameUser));
        when(userService.update(eq(userId), eq(payload))).thenReturn(Optional.of(updatedUser));

        UserResponse result = userController.updateUser(userId, payload);

        assertThat(result).isEqualTo(updatedUser);
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.role()).isEqualTo("USER");
        verify(userService, times(1)).findByEmail(sameEmail);
        verify(userService, times(1)).update(userId, payload);
    }

    @Test
    @DisplayName("updateUser throws 400 Bad Request when both name and email are null")
    void updateUserShouldThrowBadRequestWhenNoFieldsProvided() {
        int userId = 5;
        UserUpdateRequest payload = new UserUpdateRequest(null, null);

        assertThatThrownBy(() -> userController.updateUser(userId, payload))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(BAD_REQUEST);
                    assertThat(rse.getReason()).isEqualTo("Informe ao menos um campo para atualizar");
                });

        verify(userService, never()).findByEmail(any());
        verify(userService, never()).update(anyInt(), any());
    }

    @Test
    @DisplayName("updateUser throws 404 Not Found when userService.update returns empty")
    void updateUserShouldThrowNotFoundWhenUserDoesNotExist() {
        int userId = 6;
        UserUpdateRequest payload = new UserUpdateRequest("Name", "email@example.com");

        when(userService.findByEmail("email@example.com")).thenReturn(Optional.empty());
        when(userService.update(eq(userId), eq(payload))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userController.updateUser(userId, payload))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(NOT_FOUND);
                    assertThat(rse.getReason()).isEqualTo("Usuário não encontrado");
                });

        verify(userService, times(1)).findByEmail("email@example.com");
        verify(userService, times(1)).update(userId, payload);
    }

    @Test
    @DisplayName("updateUser handles payload with null and non-null fields correctly and returns user with status and role")
    void updateUserShouldHandlePayloadWithNullAndNonNullFields() {
        int userId = 7;
        UserUpdateRequest payload = new UserUpdateRequest("Valid Name", null);
        UserResponse updatedUser = new UserResponse(userId, "Valid Name", "existing@example.com", "ACTIVE", "USER");

        when(userService.update(eq(userId), eq(payload))).thenReturn(Optional.of(updatedUser));

        UserResponse result = userController.updateUser(userId, payload);

        assertThat(result).isEqualTo(updatedUser);
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.role()).isEqualTo("USER");
        verify(userService, never()).findByEmail(any());
        verify(userService, times(1)).update(userId, payload);
    }

    @Test
    @DisplayName("updateUser propagates unexpected exceptions from userService.update")
    void updateUserShouldPropagateUnexpectedExceptions() {
        int userId = 8;
        UserUpdateRequest payload = new UserUpdateRequest("Name", "email@example.com");

        when(userService.findByEmail("email@example.com")).thenReturn(Optional.empty());
        when(userService.update(eq(userId), eq(payload))).thenThrow(new RuntimeException("Unexpected error"));

        assertThatThrownBy(() -> userController.updateUser(userId, payload))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unexpected error");

        verify(userService, times(1)).findByEmail("email@example.com");
        verify(userService, times(1)).update(userId, payload);
    }

    // New tests to cover suggestions from QA report

    @Test
    @DisplayName("updateUser does not alter status and role when not specified in update")
    void updateUserShouldNotAlterStatusAndRoleWhenNotSpecified() {
        int userId = 9;
        UserUpdateRequest payload = new UserUpdateRequest("Name Updated", "emailupdated@example.com");
        // Simulate that status and role remain unchanged after update
        UserResponse updatedUser = new UserResponse(userId, "Name Updated", "emailupdated@example.com", "ACTIVE", "USER");

        when(userService.findByEmail("emailupdated@example.com")).thenReturn(Optional.empty());
        when(userService.update(eq(userId), eq(payload))).thenReturn(Optional.of(updatedUser));

        UserResponse result = userController.updateUser(userId, payload);

        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.role()).isEqualTo("USER");
    }

    @Test
    @DisplayName("updateUser returns UserResponse with different status and role values correctly propagated")
    void updateUserShouldReturnUserResponseWithDifferentStatusAndRole() {
        int userId = 10;
        UserUpdateRequest payload = new UserUpdateRequest("Name", "email@example.com");
        UserResponse updatedUser = new UserResponse(userId, "Name", "email@example.com", "INACTIVE", "ADMIN");

        when(userService.findByEmail("email@example.com")).thenReturn(Optional.empty());
        when(userService.update(eq(userId), eq(payload))).thenReturn(Optional.of(updatedUser));

        UserResponse result = userController.updateUser(userId, payload);

        assertThat(result.status()).isEqualTo("INACTIVE");
        assertThat(result.role()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("updateUser throws exception when userService.update returns UserResponse with null status and role")
    void updateUserShouldHandleUserResponseWithNullStatusAndRole() {
        int userId = 11;
        UserUpdateRequest payload = new UserUpdateRequest("Name", "email@example.com");
        UserResponse updatedUser = new UserResponse(userId, "Name", "email@example.com", null, null);

        when(userService.findByEmail("email@example.com")).thenReturn(Optional.empty());
        when(userService.update(eq(userId), eq(payload))).thenReturn(Optional.of(updatedUser));

        UserResponse result = userController.updateUser(userId, payload);

        // Assert that null status and role are propagated as null (or could be defaulted if logic exists)
        assertThat(result.status()).isNull();
        assertThat(result.role()).isNull();
    }

    @Test
    @DisplayName("updateUser throws exception when userService.update returns UserResponse with empty status and role")
    void updateUserShouldHandleUserResponseWithEmptyStatusAndRole() {
        int userId = 12;
        UserUpdateRequest payload = new UserUpdateRequest("Name", "email@example.com");
        UserResponse updatedUser = new UserResponse(userId, "Name", "email@example.com", "", "");

        when(userService.findByEmail("email@example.com")).thenReturn(Optional.empty());
        when(userService.update(eq(userId), eq(payload))).thenReturn(Optional.of(updatedUser));

        UserResponse result = userController.updateUser(userId, payload);

        assertThat(result.status()).isEmpty();
        assertThat(result.role()).isEmpty();
    }

    @Test
    @DisplayName("updateUser throws exception when userService.update throws exception related to status or role")
    void updateUserShouldPropagateExceptionFromUserServiceUpdateRelatedToStatusOrRole() {
        int userId = 13;
        UserUpdateRequest payload = new UserUpdateRequest("Name", "email@example.com");

        when(userService.findByEmail("email@example.com")).thenReturn(Optional.empty());
        when(userService.update(eq(userId), eq(payload))).thenThrow(new IllegalArgumentException("Invalid status or role"));

        assertThatThrownBy(() -> userController.updateUser(userId, payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status or role");
    }
}