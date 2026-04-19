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

    @Test
    @DisplayName("listUsers returns list from userService with given limit and offset")
    void listUsersShouldReturnUsersFromService() {
        List<UserResponse> users = List.of(
                new UserResponse(1, "Ana Silva", "ana@example.com"),
                new UserResponse(2, "Bruno Lima", "bruno@example.com")
        );
        when(userService.listUsers(10, 5)).thenReturn(users);

        List<UserResponse> result = userController.listUsers(10, 5);

        assertThat(result).isEqualTo(users);
        verify(userService, times(1)).listUsers(10, 5);
    }

    @Test
    @DisplayName("listUsers returns empty list when limit is zero")
    void listUsersShouldReturnEmptyListWhenLimitIsZero() {
        when(userService.listUsers(0, 0)).thenReturn(List.of());

        List<UserResponse> result = userController.listUsers(0, 0);

        assertThat(result).isEmpty();
        verify(userService, times(1)).listUsers(0, 0);
    }

    @Test
    @DisplayName("listUsers returns empty list when offset is negative")
    void listUsersShouldReturnEmptyListWhenOffsetIsNegative() {
        when(userService.listUsers(10, -1)).thenReturn(List.of());

        List<UserResponse> result = userController.listUsers(10, -1);

        assertThat(result).isEmpty();
        verify(userService, times(1)).listUsers(10, -1);
    }

    @Test
    @DisplayName("usersCount returns count of all users from userService")
    void usersCountShouldReturnCorrectCount() {
        List<UserResponse> allUsers = List.of(
                new UserResponse(1, "Ana Silva", "ana@example.com"),
                new UserResponse(2, "Bruno Lima", "bruno@example.com"),
                new UserResponse(3, "Carlos", "carlos@example.com")
        );
        when(userService.listAllUsers()).thenReturn(allUsers);

        var countResponse = userController.usersCount();

        assertThat(countResponse).isNotNull();
        assertThat(countResponse.count()).isEqualTo(allUsers.size());
        verify(userService, times(1)).listAllUsers();
    }

    @Test
    @DisplayName("createUser returns 201 and created user when email is new")
    void createUserShouldReturnCreatedUserForNewEmail() {
        UserCreateRequest request = new UserCreateRequest("Carlos Souza", "carlos@example.com");
        UserResponse created = new UserResponse(3, "Carlos Souza", "carlos@example.com");
        when(userService.findByEmail("carlos@example.com")).thenReturn(Optional.empty());
        when(userService.create(request)).thenReturn(created);

        UserResponse result = userController.createUser(request);

        assertThat(result).isEqualTo(created);
        verify(userService, times(1)).findByEmail("carlos@example.com");
        verify(userService, times(1)).create(request);
    }

    @Test
    @DisplayName("createUser throws 409 CONFLICT when email already exists")
    void createUserShouldThrowConflictWhenEmailAlreadyRegistered() {
        UserCreateRequest request = new UserCreateRequest("Ana Clone", "ana@example.com");
        UserResponse existing = new UserResponse(1, "Ana Silva", "ana@example.com");
        when(userService.findByEmail("ana@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userController.createUser(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(CONFLICT));

        verify(userService, never()).create(any());
    }

    @Test
    @DisplayName("createUser does not call create if findByEmail throws unexpected exception")
    void createUserShouldNotCallCreateIfFindByEmailThrows() {
        UserCreateRequest request = new UserCreateRequest("Name", "email@example.com");
        when(userService.findByEmail("email@example.com")).thenThrow(new RuntimeException("Unexpected"));

        assertThatThrownBy(() -> userController.createUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unexpected");

        verify(userService, times(1)).findByEmail("email@example.com");
        verify(userService, never()).create(any());
    }

    @Test
    @DisplayName("firstUserEmail returns the first user when list has multiple users")
    void firstUserEmailShouldReturnFirstUserWhenMultipleUsersExist() {
        UserResponse user1 = new UserResponse(1, "Ana Silva", "ana@example.com");
        UserResponse user2 = new UserResponse(2, "Bruno Lima", "bruno@example.com");
        when(userService.listAllUsers()).thenReturn(List.of(user1, user2));

        UserResponse result = userController.firstUserEmail();

        assertThat(result).isEqualTo(user1);
        verify(userService, times(1)).listAllUsers();
    }

    @Test
    @DisplayName("getUserEmail returns email when user exists")
    void getUserEmailShouldReturnEmailForExistingUser() {
        UserResponse user = new UserResponse(1, "Ana Silva", "ana@example.com");
        when(userService.getById(1)).thenReturn(Optional.of(user));

        var result = userController.getUserEmail(1);

        assertThat(result.email()).isEqualTo("ana@example.com");
    }

    @Test
    @DisplayName("getUserEmail throws 404 NOT FOUND when user does not exist")
    void getUserEmailShouldThrow404WhenUserNotFound() {
        when(userService.getById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userController.getUserEmail(99))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(NOT_FOUND));
    }

    @Test
    @DisplayName("getUser returns user when found")
    void getUserShouldReturnUserWhenFound() {
        UserResponse user = new UserResponse(1, "Ana Silva", "ana@example.com");
        when(userService.getById(1)).thenReturn(Optional.of(user));

        UserResponse result = userController.getUser(1);

        assertThat(result).isEqualTo(user);
    }

    @Test
    @DisplayName("getUser throws 404 NOT FOUND when user does not exist")
    void getUserShouldThrow404WhenNotFound() {
        when(userService.getById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userController.getUser(99))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(NOT_FOUND));
    }

    @Test
    @DisplayName("getUserAgeEstimate returns estimate from externalService when user exists")
    void getUserAgeEstimateShouldReturnEstimateForExistingUser() {
        UserResponse user = new UserResponse(1, "Ana Silva", "ana@example.com");
        AgeEstimateResponse estimate = new AgeEstimateResponse("Ana Silva", 30, 100);
        when(userService.getById(1)).thenReturn(Optional.of(user));
        when(externalService.estimateAge("Ana Silva")).thenReturn(estimate);

        AgeEstimateResponse result = userController.getUserAgeEstimate(1);

        assertThat(result).isEqualTo(estimate);
        verify(externalService, times(1)).estimateAge("Ana Silva");
    }

    @Test
    @DisplayName("getUserAgeEstimate throws 404 NOT FOUND when user does not exist")
    void getUserAgeEstimateShouldThrow404WhenUserNotFound() {
        when(userService.getById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userController.getUserAgeEstimate(99))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(NOT_FOUND));

        verifyNoInteractions(externalService);
    }

    @Test
    @DisplayName("getUserAgeEstimate propagates exception when externalService.estimateAge throws")
    void getUserAgeEstimateShouldPropagateExceptionWhenExternalServiceFails() {
        UserResponse user = new UserResponse(1, "Ana Silva", "ana@example.com");
        when(userService.getById(1)).thenReturn(Optional.of(user));
        when(externalService.estimateAge("Ana Silva")).thenThrow(new RuntimeException("External failure"));

        assertThatThrownBy(() -> userController.getUserAgeEstimate(1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("External failure");

        verify(externalService, times(1)).estimateAge("Ana Silva");
    }

    @Test
    @DisplayName("findDuplicateUsers returns duplicates based on email")
    void findDuplicateUsersShouldReturnUsersWithDuplicateEmails() {
        UserResponse user1 = new UserResponse(1, "Ana Silva", "ana@example.com");
        UserResponse user2 = new UserResponse(2, "Bruno Lima", "bruno@example.com");
        UserResponse user3 = new UserResponse(3, "Ana Silva Duplicate", "ana@example.com");
        List<UserResponse> allUsers = List.of(user1, user2, user3);
        when(userService.listAllUsers()).thenReturn(allUsers);

        List<UserResponse> duplicates = userController.findDuplicateUsers();

        assertThat(duplicates).containsExactlyInAnyOrder(user1, user3);
        verify(userService, times(1)).listAllUsers();
    }

    @Test
    @DisplayName("findDuplicateUsers returns empty list when no duplicates exist")
    void findDuplicateUsersShouldReturnEmptyListWhenNoDuplicates() {
        UserResponse user1 = new UserResponse(1, "Ana Silva", "ana@example.com");
        UserResponse user2 = new UserResponse(2, "Bruno Lima", "bruno@example.com");
        List<UserResponse> allUsers = List.of(user1, user2);
        when(userService.listAllUsers()).thenReturn(allUsers);

        List<UserResponse> duplicates = userController.findDuplicateUsers();

        assertThat(duplicates).isEmpty();
        verify(userService, times(1)).listAllUsers();
    }

    @Test
    @DisplayName("searchUsers returns users whose names contain search term case-insensitively")
    void searchUsersShouldFilterUsersByName() {
        UserResponse user1 = new UserResponse(1, "Ana Silva", "ana@example.com");
        UserResponse user2 = new UserResponse(2, "Bruno Lima", "bruno@example.com");
        UserResponse user3 = new UserResponse(3, "Carlos", "carlos@example.com");
        List<UserResponse> allUsers = List.of(user1, user2, user3);
        when(userService.listAllUsers()).thenReturn(allUsers);

        List<UserResponse> result = userController.searchUsers("an");

        assertThat(result).containsExactly(user1);
        verify(userService, times(1)).listAllUsers();
    }

    @Test
    @DisplayName("searchUsers returns empty list when no users match search term")
    void searchUsersShouldReturnEmptyListWhenNoMatch() {
        UserResponse user1 = new UserResponse(1, "Ana Silva", "ana@example.com");
        UserResponse user2 = new UserResponse(2, "Bruno Lima", "bruno@example.com");
        List<UserResponse> allUsers = List.of(user1, user2);
        when(userService.listAllUsers()).thenReturn(allUsers);

        List<UserResponse> result = userController.searchUsers("xyz");

        assertThat(result).isEmpty();
        verify(userService, times(1)).listAllUsers();
    }

}