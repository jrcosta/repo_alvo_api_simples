package com.repoalvo.javaapi;

import com.repoalvo.javaapi.model.UserCreateRequest;
import com.repoalvo.javaapi.model.UserUpdateRequest;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceUnitTest {

    private UserService userService;

    @BeforeEach
    void setup() {
        userService = new UserService();
    }

    @Test
    void create_shouldAssignDefaultRoleUserAndStatusActive_whenRoleIsNull() {
        UserCreateRequest payload = new UserCreateRequest("Carlos", "carlos@example.com", null);

        UserResponse created = userService.create(payload);

        assertThat(created).isNotNull();
        assertThat(created.id()).isGreaterThan(0);
        assertThat(created.name()).isEqualTo("Carlos");
        assertThat(created.email()).isEqualTo("carlos@example.com");
        assertThat(created.status()).isEqualTo("ACTIVE");
        assertThat(created.role()).isEqualTo("USER");
    }

    @Test
    void create_shouldAssignRoleFromPayloadAndStatusActive_whenRoleIsDefined() {
        UserCreateRequest payload = new UserCreateRequest("Diana", "diana@example.com", "ADMIN");

        UserResponse created = userService.create(payload);

        assertThat(created).isNotNull();
        assertThat(created.id()).isGreaterThan(0);
        assertThat(created.name()).isEqualTo("Diana");
        assertThat(created.email()).isEqualTo("diana@example.com");
        assertThat(created.status()).isEqualTo("ACTIVE");
        assertThat(created.role()).isEqualTo("ADMIN");
    }

    @Test
    void update_shouldPreserveStatusAndRole_whenUpdatingNameAndEmail() {
        int userId = 1;
        Optional<UserResponse> existingOpt = userService.getById(userId);
        assertThat(existingOpt).isPresent();
        UserResponse existing = existingOpt.get();

        UserUpdateRequest payload = new UserUpdateRequest("Ana Updated", "ana.updated@example.com");

        Optional<UserResponse> updatedOpt = userService.update(userId, payload);

        assertThat(updatedOpt).isPresent();
        UserResponse updated = updatedOpt.get();

        assertThat(updated.id()).isEqualTo(userId);
        assertThat(updated.name()).isEqualTo("Ana Updated");
        assertThat(updated.email()).isEqualTo("ana.updated@example.com");
        assertThat(updated.status()).isEqualTo(existing.status());
        assertThat(updated.role()).isEqualTo(existing.role());
    }

    @Test
    void update_shouldReturnEmptyOptional_whenUserDoesNotExist() {
        int nonExistentUserId = 9999;
        UserUpdateRequest payload = new UserUpdateRequest("Non Existent", "nonexistent@example.com");

        Optional<UserResponse> updatedOpt = userService.update(nonExistentUserId, payload);

        assertThat(updatedOpt).isEmpty();
    }

    @Test
    void constructor_shouldInitializeUsersWithCorrectStatusAndRole() {
        // The constructor adds two users with specific status and role
        var allUsers = userService.listAllUsers();

        assertThat(allUsers).hasSizeGreaterThanOrEqualTo(2);

        UserResponse user1 = allUsers.stream().filter(u -> u.id() == 1).findFirst().orElse(null);
        UserResponse user2 = allUsers.stream().filter(u -> u.id() == 2).findFirst().orElse(null);

        assertThat(user1).isNotNull();
        assertThat(user1.status()).isEqualTo("ACTIVE");
        assertThat(user1.role()).isEqualTo("ADMIN");

        assertThat(user2).isNotNull();
        assertThat(user2.status()).isEqualTo("ACTIVE");
        assertThat(user2.role()).isEqualTo("USER");
    }
}