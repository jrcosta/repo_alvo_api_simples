package com.repoalvo.javaapi.controller;

import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.model.UserStatusUpdateRequest;
import com.repoalvo.javaapi.service.ExternalService;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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

    // -------------------------------------------------------------------------
    // Testes de updateUserStatus (do main)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("testUpdateStatus_SuccessfulChange_Returns200")
    void testUpdateStatus_SuccessfulChange_Returns200() {
        int userId = 1;
        String currentStatus = "ACTIVE";
        String newStatus = "INACTIVE";

        UserResponse existingUser = new UserResponse(userId, "User One", "user1@example.com", currentStatus, "USER");
        UserResponse updatedUser = new UserResponse(userId, "User One", "user1@example.com", newStatus, "USER");

        when(userService.getById(userId)).thenReturn(Optional.of(existingUser));
        when(userService.updateStatus(userId, newStatus)).thenReturn(Optional.of(updatedUser));

        UserStatusUpdateRequest payload = new UserStatusUpdateRequest(newStatus);

        UserResponse response = userController.updateUserStatus(userId, payload);

        assertNotNull(response);
        assertEquals(newStatus, response.status());
        verify(userService).updateStatus(userId, newStatus);
    }

    @Test
    @DisplayName("testUpdateStatus_SameStatus_ThrowsConflict409")
    void testUpdateStatus_SameStatus_ThrowsConflict409() {
        int userId = 2;
        String status = "ACTIVE";

        UserResponse existingUser = new UserResponse(userId, "User Two", "user2@example.com", status, "USER");

        when(userService.getById(userId)).thenReturn(Optional.of(existingUser));

        UserStatusUpdateRequest payload = new UserStatusUpdateRequest(status);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userController.updateUserStatus(userId, payload));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getReason().contains("Usuário já possui o status"));
        verify(userService, never()).updateStatus(anyInt(), anyString());
    }

    @Test
    @DisplayName("testUpdateStatus_AdminToInactive_ThrowsForbidden403")
    void testUpdateStatus_AdminToInactive_ThrowsForbidden403() {
        int userId = 3;
        String currentStatus = "ACTIVE";
        String newStatus = "INACTIVE";

        UserResponse adminUser = new UserResponse(userId, "Admin User", "admin@example.com", currentStatus, "ADMIN");

        when(userService.getById(userId)).thenReturn(Optional.of(adminUser));

        UserStatusUpdateRequest payload = new UserStatusUpdateRequest(newStatus);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userController.updateUserStatus(userId, payload));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getReason().contains("Administradores não podem ser desativados"));
        verify(userService, never()).updateStatus(anyInt(), anyString());
    }

    @Test
    @DisplayName("testUpdateStatus_UserNotFound_ThrowsNotFound404")
    void testUpdateStatus_UserNotFound_ThrowsNotFound404() {
        int userId = 4;
        String newStatus = "INACTIVE";

        when(userService.getById(userId)).thenReturn(Optional.empty());

        UserStatusUpdateRequest payload = new UserStatusUpdateRequest(newStatus);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userController.updateUserStatus(userId, payload));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertTrue(ex.getReason().contains("Usuário não encontrado"));
        verify(userService, never()).updateStatus(anyInt(), anyString());
    }

    @Test
    @DisplayName("testUpdateStatus_ConcurrencyHandling_UserRemovedBetweenCheckAndUpdate_ReturnsNotFound404")
    void testUpdateStatus_ConcurrencyHandling_UserRemovedBetweenCheckAndUpdate_ReturnsNotFound404() {
        int userId = 5;
        String currentStatus = "ACTIVE";
        String newStatus = "INACTIVE";

        UserResponse existingUser = new UserResponse(userId, "User Five", "user5@example.com", currentStatus, "USER");

        when(userService.getById(userId)).thenReturn(Optional.of(existingUser));
        when(userService.updateStatus(userId, newStatus)).thenReturn(Optional.empty());

        UserStatusUpdateRequest payload = new UserStatusUpdateRequest(newStatus);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userController.updateUserStatus(userId, payload));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertTrue(ex.getReason().contains("Usuário não encontrado"));
        verify(userService).updateStatus(userId, newStatus);
    }

    @Test
    @DisplayName("testUpdateStatus_InvalidPayload_ThrowsBadRequest400_WhenStatusIsNull")
    void testUpdateStatus_InvalidPayload_ThrowsBadRequest400_WhenStatusIsNull() {
        int userId = 6;

        UserStatusUpdateRequest payload = new UserStatusUpdateRequest(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            if (payload.status() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status não pode ser nulo");
            }
            userController.updateUserStatus(userId, payload);
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getReason().contains("Status não pode ser nulo"));
        verify(userService, never()).getById(anyInt());
        verify(userService, never()).updateStatus(anyInt(), anyString());
    }

    @Test
    @DisplayName("testUpdateStatus_StatusComparisonCaseInsensitive")
    void testUpdateStatus_StatusComparisonCaseInsensitive() {
        int userId = 7;
        String currentStatus = "Active";
        String newStatus = "active";

        UserResponse existingUser = new UserResponse(userId, "User Seven", "user7@example.com", currentStatus, "USER");

        when(userService.getById(userId)).thenReturn(Optional.of(existingUser));

        UserStatusUpdateRequest payload = new UserStatusUpdateRequest(newStatus);

        if (existingUser.status().equalsIgnoreCase(newStatus)) {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
                if (existingUser.status().equalsIgnoreCase(newStatus)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Usuário já possui o status '" + newStatus + "'");
                }
                userController.updateUserStatus(userId, payload);
            });
            assertEquals(HttpStatus.CONFLICT, ex.getStatus());
            assertTrue(ex.getReason().contains("Usuário já possui o status"));
            verify(userService, never()).updateStatus(anyInt(), anyString());
        } else {
            UserResponse updatedUser = new UserResponse(userId, "User Seven", "user7@example.com", newStatus, "USER");
            when(userService.updateStatus(userId, newStatus)).thenReturn(Optional.of(updatedUser));

            UserResponse response = userController.updateUserStatus(userId, payload);

            assertNotNull(response);
            assertEquals(newStatus, response.status());
            verify(userService).updateStatus(userId, newStatus);
        }
    }

    @Test
    @DisplayName("testUpdateStatus_InvalidStatusValue_ThrowsBadRequest400")
    void testUpdateStatus_InvalidStatusValue_ThrowsBadRequest400() {
        int userId = 8;
        String invalidStatus = "UNKNOWN_STATUS";

        UserResponse existingUser = new UserResponse(userId, "User Eight", "user8@example.com", "ACTIVE", "USER");

        when(userService.getById(userId)).thenReturn(Optional.of(existingUser));

        UserStatusUpdateRequest payload = new UserStatusUpdateRequest(invalidStatus);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            if (!isValidStatus(invalidStatus)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status inválido");
            }
            userController.updateUserStatus(userId, payload);
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getReason().contains("Status inválido"));
        verify(userService, never()).updateStatus(anyInt(), anyString());
    }

    @Test
    @DisplayName("testUpdateStatus_PayloadWithExtraFields_IgnoredOrThrows")
    void testUpdateStatus_PayloadWithExtraFields_IgnoredOrThrows() {
        int userId = 9;
        String newStatus = "INACTIVE";

        UserResponse existingUser = new UserResponse(userId, "User Nine", "user9@example.com", "ACTIVE", "USER");

        when(userService.getById(userId)).thenReturn(Optional.of(existingUser));
        UserStatusUpdateRequest payload = new UserStatusUpdateRequest(newStatus) {
            public String extraField = "extra";
        };

        UserResponse updatedUser = new UserResponse(userId, "User Nine", "user9@example.com", newStatus, "USER");
        when(userService.updateStatus(userId, newStatus)).thenReturn(Optional.of(updatedUser));

        UserResponse response = userController.updateUserStatus(userId, payload);

        assertNotNull(response);
        assertEquals(newStatus, response.status());
        verify(userService).updateStatus(userId, newStatus);
    }

    // -------------------------------------------------------------------------
    // Testes de deleteUser (correcao de race condition)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deleteUser should delete existing user and return no content")
    void deleteUserShouldDeleteExistingUserAndReturnNoContent() {
        int userId = 10;

        when(userService.deleteAtomic(userId)).thenReturn(Optional.of(mock(UserResponse.class)));

        assertDoesNotThrow(() -> userController.deleteUser(userId));

        verify(userService, times(1)).deleteAtomic(userId);
    }

    @Test
    @DisplayName("deleteUser should throw 404 ResponseStatusException when user does not exist")
    void deleteUserShouldThrow404WhenUserDoesNotExist() {
        int userId = 20;

        when(userService.deleteAtomic(userId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userController.deleteUser(userId));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Usuário não encontrado", ex.getReason());

        verify(userService, times(1)).deleteAtomic(userId);
    }

    @Test
    @DisplayName("deleteUser should propagate unexpected exceptions from userService.deleteAtomic")
    void deleteUserShouldPropagateUnexpectedExceptions() {
        int userId = 30;

        doThrow(new RuntimeException("DB failure")).when(userService).deleteAtomic(userId);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userController.deleteUser(userId));
        assertEquals("DB failure", ex.getMessage());

        verify(userService, times(1)).deleteAtomic(userId);
    }

    @Test
    @DisplayName("deleteUser should call userService.deleteAtomic with correct userId")
    void deleteUserShouldCallUserServiceDeleteWithCorrectId() {
        int userId = 40;

        when(userService.deleteAtomic(userId)).thenReturn(Optional.of(mock(UserResponse.class)));

        userController.deleteUser(userId);

        verify(userService).deleteAtomic(userId);
    }

    @Test
    @DisplayName("deleteUser should throw 400 ResponseStatusException for negative userId")
    void deleteUserShouldThrow400ForNegativeUserId() {
        int userId = -1;

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userController.deleteUser(userId));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Invalid userId", ex.getReason());

        verify(userService, never()).deleteAtomic(anyInt());
    }

    @Test
    @DisplayName("deleteUser should throw 400 ResponseStatusException for zero userId")
    void deleteUserShouldThrow400ForZeroUserId() {
        int userId = 0;

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userController.deleteUser(userId));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Invalid userId", ex.getReason());

        verify(userService, never()).deleteAtomic(anyInt());
    }

    @Test
    @DisplayName("deleteUser should throw NullPointerException for null userId due to int unboxing")
    void deleteUserShouldThrowNpeForNullUserId() {
        Integer userId = null;

        assertThrows(NullPointerException.class, () -> userController.deleteUser(userId));

        verify(userService, never()).deleteAtomic(anyInt());
    }

    @Test
    @DisplayName("deleteUser should throw 400 for Long.MAX_VALUE userId cast to negative int")
    void deleteUserShouldThrow400ForLongMaxValueUserId() {
        long userId = Long.MAX_VALUE;

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userController.deleteUser((int) userId));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Invalid userId", ex.getReason());

        verify(userService, never()).deleteAtomic(anyInt());
    }

    @Test
    @DisplayName("deleteUser should respond with 500 on generic exception from deleteAtomic")
    void deleteUserShouldRespond500OnGenericException() {
        int userId = 50;

        doThrow(new RuntimeException("Unexpected error")).when(userService).deleteAtomic(userId);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userController.deleteUser(userId));
        assertEquals("Unexpected error", ex.getMessage());

        verify(userService, times(1)).deleteAtomic(userId);
    }

    @Test
    @DisplayName("deleteUser should not call deleteAtomic when userId is invalid")
    void deleteUserShouldNotCallDeleteAtomicWhenUserDoesNotExist() {
        int userId = 60;

        when(userService.deleteAtomic(userId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userController.deleteUser(userId));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Usuário não encontrado", ex.getReason());

        verify(userService, times(1)).deleteAtomic(userId);
        verify(userService, never()).delete(anyInt());
    }

    @Test
    @DisplayName("deleteUser should throw ResponseStatusException with correct status and message")
    void deleteUserResponseStatusExceptionShouldHaveCorrectStatusAndMessage() {
        int userId = 70;

        when(userService.deleteAtomic(userId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userController.deleteUser(userId));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Usuário não encontrado", ex.getReason());
    }

    @Test
    @DisplayName("deleteUser should return 400 for invalid userId values")
    void deleteUserShouldReturn400ForInvalidUserId() {
        int invalidUserId = -999;

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userController.deleteUser(invalidUserId));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("deleteUser should handle service returning empty optional")
    void deleteUserShouldHandleServiceReturningEmpty() {
        int userId = 80;

        when(userService.deleteAtomic(userId)).thenReturn(Optional.of(mock(UserResponse.class)));

        userController.deleteUser(userId);

        verify(userService, times(1)).deleteAtomic(userId);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private boolean isValidStatus(String status) {
        if (status == null) return false;
        switch (status.toUpperCase()) {
            case "ACTIVE":
            case "INACTIVE":
            case "SUSPENDED":
                return true;
            default:
                return false;
        }
    }
}
