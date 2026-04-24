package com.repoalvo.javaapi;

import com.repoalvo.javaapi.model.UserUpdateRequest;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceUnitTest {

    private UserService userService;

    @BeforeEach
    void setup() {
        userService = new UserService();
    }

    @Test
    void update_shouldReturnUpdatedUser_whenUserExistsAndPayloadHasNameAndEmail() {
        // Arrange
        int userId = 1;
        String newName = "Ana Updated";
        String newEmail = "ana.updated@example.com";
        UserUpdateRequest payload = new UserUpdateRequest(newName, newEmail);

        // Act
        Optional<UserResponse> updatedOpt = userService.update(userId, payload);

        // Assert
        assertThat(updatedOpt).isPresent();
        UserResponse updated = updatedOpt.get();
        assertThat(updated.id()).isEqualTo(userId);
        assertThat(updated.name()).isEqualTo(newName);
        assertThat(updated.email()).isEqualTo(newEmail);
    }

    @Test
    void update_shouldReturnUpdatedUser_whenPayloadHasOnlyName() {
        // Arrange
        int userId = 2;
        String originalEmail = userService.getById(userId).map(UserResponse::email).orElse(null);
        String newName = "Bruno Updated";
        UserUpdateRequest payload = new UserUpdateRequest(newName, null);

        // Act
        Optional<UserResponse> updatedOpt = userService.update(userId, payload);

        // Assert
        assertThat(updatedOpt).isPresent();
        UserResponse updated = updatedOpt.get();
        assertThat(updated.id()).isEqualTo(userId);
        assertThat(updated.name()).isEqualTo(newName);
        assertThat(updated.email()).isEqualTo(originalEmail);
    }

    @Test
    void update_shouldReturnUpdatedUser_whenPayloadHasOnlyEmail() {
        // Arrange
        int userId = 1;
        String originalName = userService.getById(userId).map(UserResponse::name).orElse(null);
        String newEmail = "ana.newemail@example.com";
        UserUpdateRequest payload = new UserUpdateRequest(null, newEmail);

        // Act
        Optional<UserResponse> updatedOpt = userService.update(userId, payload);

        // Assert
        assertThat(updatedOpt).isPresent();
        UserResponse updated = updatedOpt.get();
        assertThat(updated.id()).isEqualTo(userId);
        assertThat(updated.name()).isEqualTo(originalName);
        assertThat(updated.email()).isEqualTo(newEmail);
    }

    @Test
    void update_shouldReturnEmpty_whenUserDoesNotExist() {
        // Arrange
        int nonExistentUserId = 9999;
        UserUpdateRequest payload = new UserUpdateRequest("Name", "email@example.com");

        // Act
        Optional<UserResponse> updatedOpt = userService.update(nonExistentUserId, payload);

        // Assert
        assertThat(updatedOpt).isEmpty();
    }

    @Test
    void update_shouldNotModifyUser_whenPayloadHasNullFields() {
        // Arrange
        int userId = 1;
        UserResponse original = userService.getById(userId).orElseThrow();
        UserUpdateRequest payload = new UserUpdateRequest(null, null);

        // Act
        Optional<UserResponse> updatedOpt = userService.update(userId, payload);

        // Assert
        assertThat(updatedOpt).isPresent();
        UserResponse updated = updatedOpt.get();
        assertThat(updated.id()).isEqualTo(userId);
        assertThat(updated.name()).isEqualTo(original.name());
        assertThat(updated.email()).isEqualTo(original.email());
    }

    @Test
    void update_shouldReplaceUserInList() {
        // Arrange
        int userId = 2;
        UserResponse beforeUpdate = userService.getById(userId).orElseThrow();
        UserUpdateRequest payload = new UserUpdateRequest("New Name", "newemail@example.com");

        // Act
        Optional<UserResponse> updatedOpt = userService.update(userId, payload);
        UserResponse afterUpdate = userService.getById(userId).orElseThrow();

        // Assert
        assertThat(updatedOpt).isPresent();
        UserResponse updated = updatedOpt.get();
        assertThat(updated).isEqualTo(afterUpdate);
        assertThat(updated).isNotEqualTo(beforeUpdate);
        assertThat(updated.name()).isEqualTo("New Name");
        assertThat(updated.email()).isEqualTo("newemail@example.com");
    }

    @Test
    void update_shouldBeThreadSafe() throws InterruptedException {
        // Arrange
        int userId = 1;
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        Runnable updateTask = () -> {
            try {
                startLatch.await();
                String threadName = Thread.currentThread().getName();
                UserUpdateRequest payload = new UserUpdateRequest("Name " + threadName, "email" + threadName + "@example.com");
                Optional<UserResponse> updatedOpt = userService.update(userId, payload);
                if (updatedOpt.isPresent()) {
                    successCount.incrementAndGet();
                }
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        };

        for (int i = 0; i < threadCount; i++) {
            new Thread(updateTask, "T" + i).start();
        }

        // Act
        startLatch.countDown();
        doneLatch.await();

        // Assert
        assertThat(successCount.get()).isEqualTo(threadCount);

        // The final state should be one of the updates, no exceptions or corrupt state
        Optional<UserResponse> finalUserOpt = userService.getById(userId);
        assertThat(finalUserOpt).isPresent();
        UserResponse finalUser = finalUserOpt.get();
        assertThat(finalUser.name()).startsWith("Name T");
        assertThat(finalUser.email()).endsWith("@example.com");
    }
}