package com.repoalvo.javaapi;

import com.repoalvo.javaapi.model.UserCreateRequest;
import com.repoalvo.javaapi.model.UserUpdateRequest;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceUnitTest {

    private UserService userService;

    @BeforeEach
    void setup() {
        userService = new UserService();
    }

    @Test
    void createUser_shouldIncludePhoneNumber_whenPhoneNumberIsProvided() {
        // Arrange
        UserCreateRequest payload = new UserCreateRequest("Carlos", "carlos@example.com", "USER", "+55 11 91234-5678");

        // Act
        UserResponse createdUser = userService.create(payload);

        // Assert
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.name()).isEqualTo("Carlos");
        assertThat(createdUser.email()).isEqualTo("carlos@example.com");
        assertThat(createdUser.role()).isEqualTo("USER");
        assertThat(createdUser.phoneNumber()).isEqualTo("+55 11 91234-5678");
    }

    @Test
    void createUser_shouldHaveNullPhoneNumber_whenPhoneNumberIsNull() {
        // Arrange
        UserCreateRequest payload = new UserCreateRequest("Daniela", "daniela@example.com", "USER", null);

        // Act
        UserResponse createdUser = userService.create(payload);

        // Assert
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.name()).isEqualTo("Daniela");
        assertThat(createdUser.email()).isEqualTo("daniela@example.com");
        assertThat(createdUser.phoneNumber()).isNull();
    }

    @Test
    void createUser_shouldHaveEmptyPhoneNumber_whenPhoneNumberIsEmptyString() {
        // Arrange
        UserCreateRequest payload = new UserCreateRequest("Eduardo", "eduardo@example.com", "USER", "");

        // Act
        UserResponse createdUser = userService.create(payload);

        // Assert
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.phoneNumber()).isEmpty();
    }

    @Test
    void listUsers_shouldReturnUsersWithCorrectPhoneNumbers_includingSeededUsers() {
        // Act
        List<UserResponse> users = userService.listAllUsers();

        // Assert
        assertThat(users).isNotEmpty();
        assertThat(users).anyMatch(u -> "+55 11 90000-0001".equals(u.phoneNumber()));
        assertThat(users).anyMatch(u -> "+55 11 90000-0002".equals(u.phoneNumber()));

        // Create a new user with phone and check presence
        UserCreateRequest payload = new UserCreateRequest("Felipe", "felipe@example.com", "USER", "+55 11 99999-9999");
        UserResponse created = userService.create(payload);

        List<UserResponse> updatedUsers = userService.listAllUsers();
        assertThat(updatedUsers).contains(created);
        assertThat(created.phoneNumber()).isEqualTo("+55 11 99999-9999");
    }

    @Test
    void updateUser_shouldNotChangePhoneNumber_evenIfPayloadContainsPhoneNumber() {
        // Arrange
        int userId = 1;
        UserResponse originalUser = userService.getById(userId).orElseThrow();
        String originalPhone = originalUser.phoneNumber();

        // Payload with phoneNumber (even if UserUpdateRequest had it, service ignores)
        // Since UserUpdateRequest constructor in code has only name and email, simulate with null phoneNumber
        UserUpdateRequest payload = new UserUpdateRequest("Ana Updated", "ana.updated@example.com");

        // Act
        Optional<UserResponse> updatedOpt = userService.update(userId, payload);

        // Assert
        assertThat(updatedOpt).isPresent();
        UserResponse updated = updatedOpt.get();
        assertThat(updated.name()).isEqualTo("Ana Updated");
        assertThat(updated.email()).isEqualTo("ana.updated@example.com");
        assertThat(updated.phoneNumber()).isEqualTo(originalPhone);
    }

    @Test
    void updateUser_shouldNotChangePhoneNumber_whenPayloadHasNullNameAndEmail() {
        // Arrange
        int userId = 2;
        UserResponse originalUser = userService.getById(userId).orElseThrow();
        String originalPhone = originalUser.phoneNumber();

        UserUpdateRequest payload = new UserUpdateRequest(null, null);

        // Act
        Optional<UserResponse> updatedOpt = userService.update(userId, payload);

        // Assert
        assertThat(updatedOpt).isPresent();
        UserResponse updated = updatedOpt.get();
        assertThat(updated.name()).isEqualTo(originalUser.name());
        assertThat(updated.email()).isEqualTo(originalUser.email());
        assertThat(updated.phoneNumber()).isEqualTo(originalPhone);
    }

    @Test
    void userResponseConstructor_shouldSetAllFieldsIncludingPhoneNumber() {
        // Arrange
        int id = 10;
        String name = "Gustavo";
        String email = "gustavo@example.com";
        String status = "ACTIVE";
        String role = "ADMIN";
        String phoneNumber = "+55 11 98888-7777";

        // Act
        UserResponse user = new UserResponse(id, name, email, status, role, phoneNumber);

        // Assert
        assertThat(user.id()).isEqualTo(id);
        assertThat(user.name()).isEqualTo(name);
        assertThat(user.email()).isEqualTo(email);
        assertThat(user.status()).isEqualTo(status);
        assertThat(user.role()).isEqualTo(role);
        assertThat(user.phoneNumber()).isEqualTo(phoneNumber);
    }
}