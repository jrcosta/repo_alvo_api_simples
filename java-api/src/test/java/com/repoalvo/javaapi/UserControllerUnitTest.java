package com.repoalvo.javaapi;

import com.repoalvo.javaapi.controller.UserController;
import com.repoalvo.javaapi.model.AgeEstimateResponse;
import com.repoalvo.javaapi.model.UserCreateRequest;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.service.ExternalService;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

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

    // Existing tests omitted for brevity...

    @Test
    @DisplayName("firstUserEmail throws 404 NOT FOUND when user list is empty")
    void firstUserEmailShouldThrow404WhenUserListIsEmpty() {
        when(userService.listAllUsers()).thenReturn(List.of());

        assertThatThrownBy(() -> userController.firstUserEmail())
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(NOT_FOUND));

        verify(userService, times(1)).listAllUsers();
    }

    @Test
    @DisplayName("createUser propagates non-RuntimeException from findByEmail and does not call create")
    void createUserShouldPropagateCheckedExceptionFromFindByEmail() {
        UserCreateRequest request = new UserCreateRequest("Checked Exception User", "checked@example.com");
        // Simulate a checked exception (e.g., Exception) thrown by findByEmail
        when(userService.findByEmail("checked@example.com")).thenThrow(new Exception("Checked exception"));

        assertThatThrownBy(() -> userController.createUser(request))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("Checked exception");

        verify(userService, times(1)).findByEmail("checked@example.com");
        verify(userService, never()).create(any());
    }

    @Test
    @DisplayName("listUsers returns empty list when limit is negative")
    void listUsersShouldReturnEmptyListWhenLimitIsNegative() {
        when(userService.listUsers(-5, 0)).thenReturn(List.of());

        List<UserResponse> result = userController.listUsers(-5, 0);

        assertThat(result).isEmpty();
        verify(userService, times(1)).listUsers(-5, 0);
    }

    @Test
    @DisplayName("listUsers returns empty list when limit and offset are negative")
    void listUsersShouldReturnEmptyListWhenLimitAndOffsetAreNegative() {
        when(userService.listUsers(-3, -7)).thenReturn(List.of());

        List<UserResponse> result = userController.listUsers(-3, -7);

        assertThat(result).isEmpty();
        verify(userService, times(1)).listUsers(-3, -7);
    }

    @Test
    @DisplayName("getUserAgeEstimate throws 404 NOT FOUND when user does not exist and externalService is not called")
    void getUserAgeEstimateShouldThrow404WhenUserNotFoundAndNotCallExternalService() {
        when(userService.getById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userController.getUserAgeEstimate(999))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(NOT_FOUND));

        verifyNoInteractions(externalService);
    }

    @Test
    @DisplayName("getUserAgeEstimate propagates exception from externalService even if user exists")
    void getUserAgeEstimateShouldPropagateExceptionWhenExternalServiceFailsWithUserPresent() {
        UserResponse user = new UserResponse(10, "Exception User", "exception@example.com");
        when(userService.getById(10)).thenReturn(Optional.of(user));
        when(externalService.estimateAge("Exception User")).thenThrow(new RuntimeException("External failure"));

        assertThatThrownBy(() -> userController.getUserAgeEstimate(10))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("External failure");

        verify(externalService, times(1)).estimateAge("Exception User");
    }

    @Test
    @DisplayName("searchUsers returns users matching search term with special characters and case-insensitive")
    void searchUsersShouldReturnUsersMatchingSpecialCharactersAndCaseInsensitive() {
        UserResponse user1 = new UserResponse(1, "Ana Silva", "ana@example.com");
        UserResponse user2 = new UserResponse(2, "Bruno Lima", "bruno@example.com");
        UserResponse user3 = new UserResponse(3, "Carlos", "carlos@example.com");
        UserResponse user4 = new UserResponse(4, "Ánna-Maria", "anna@example.com");
        List<UserResponse> allUsers = List.of(user1, user2, user3, user4);
        when(userService.listAllUsers()).thenReturn(allUsers);

        // Search term with uppercase and special character
        List<UserResponse> result = userController.searchUsers("ÁN");

        // Should match user4 (Ánna-Maria) case-insensitively and ignoring accents if applicable
        assertThat(result).containsExactly(user4);
        verify(userService, times(1)).listAllUsers();
    }

}