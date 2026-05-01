package com.repoalvo.javaapi;

import com.repoalvo.javaapi.model.UserCreateRequest;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.model.UserUpdateRequest;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceUnitTest {

    private UserService userService;

    @BeforeEach
    public void setup() {
        userService = new UserService();
        userService.reset();
    }

    @Test
    public void testUpdateStatus_ValidUserIdAndValidStatus_ShouldUpdateStatusOnly() {
        int userId = 1;
        String newStatus = "INACTIVE";

        Optional<UserResponse> updatedOpt = userService.updateStatus(userId, newStatus);
        assertTrue(updatedOpt.isPresent(), "User should be found and updated");

        UserResponse updated = updatedOpt.get();
        assertEquals(userId, updated.id());
        assertEquals(newStatus, updated.status());

        // Verify other fields remain unchanged
        UserResponse original = userService.getById(userId).orElseThrow();
        assertEquals(updated.name(), original.name());
        assertEquals(updated.email(), original.email());
        assertEquals(updated.role(), original.role());
        assertEquals(updated.phoneNumber(), original.phoneNumber());
    }

    @Test
    public void testUpdateStatus_ValidUserIdAndNullOrEmptyStatus_ShouldAcceptOrHandleGracefully() {
        int userId = 2;

        // Test with null status
        Optional<UserResponse> updatedNull = userService.updateStatus(userId, null);
        assertTrue(updatedNull.isPresent(), "User should be found and updated with null status");
        assertNull(updatedNull.get().status(), "Status should be set to null");

        // Test with empty string status
        Optional<UserResponse> updatedEmpty = userService.updateStatus(userId, "");
        assertTrue(updatedEmpty.isPresent(), "User should be found and updated with empty status");
        assertEquals("", updatedEmpty.get().status(), "Status should be set to empty string");
    }

    @Test
    public void testUpdateStatus_InvalidUserId_ShouldReturnEmptyOptional() {
        int invalidUserId = 9999;
        Optional<UserResponse> result = userService.updateStatus(invalidUserId, "ACTIVE");
        assertTrue(result.isEmpty(), "Updating status for invalid userId should return Optional.empty()");
    }

    @Test
    public void testUpdateStatus_OtherUserFieldsRemainUnchanged() {
        int userId = 1;
        UserResponse beforeUpdate = userService.getById(userId).orElseThrow();

        String newStatus = "INACTIVE";
        Optional<UserResponse> updatedOpt = userService.updateStatus(userId, newStatus);
        assertTrue(updatedOpt.isPresent());

        UserResponse updated = updatedOpt.get();

        // Check only status changed
        assertEquals(newStatus, updated.status());
        assertEquals(beforeUpdate.id(), updated.id());
        assertEquals(beforeUpdate.name(), updated.name());
        assertEquals(beforeUpdate.email(), updated.email());
        assertEquals(beforeUpdate.role(), updated.role());
        assertEquals(beforeUpdate.phoneNumber(), updated.phoneNumber());
    }

    @Test
    public void testUpdateStatus_IntegrationWithUpdateDeleteListOperations() {
        // Create a new user
        UserCreateRequest createRequest = new UserCreateRequest("Carlos", "carlos@example.com", "USER", "+55 11 90000-0003");
        UserResponse created = userService.create(createRequest);

        // Update status
        String newStatus = "INACTIVE";
        Optional<UserResponse> updatedOpt = userService.updateStatus(created.id(), newStatus);
        assertTrue(updatedOpt.isPresent());
        assertEquals(newStatus, updatedOpt.get().status());

        // Update general info
        UserUpdateRequest updateRequest = new UserUpdateRequest("Carlos Updated", null, null, null);
        Optional<UserResponse> updatedGeneralOpt = userService.update(created.id(), updateRequest);
        assertTrue(updatedGeneralOpt.isPresent());
        assertEquals("Carlos Updated", updatedGeneralOpt.get().name());
        assertEquals(newStatus, updatedGeneralOpt.get().status(), "Status should remain as updated");

        // List users and verify presence
        List<UserResponse> allUsers = userService.listAllUsers();
        assertTrue(allUsers.stream().anyMatch(u -> u.id() == created.id()));

        // Delete user
        userService.delete(created.id());
        Optional<UserResponse> afterDelete = userService.getById(created.id());
        assertTrue(afterDelete.isEmpty(), "User should be deleted");

        // List users and verify absence
        List<UserResponse> afterDeleteList = userService.listAllUsers();
        assertFalse(afterDeleteList.stream().anyMatch(u -> u.id() == created.id()));
    }

    @Test
    public void testUpdateStatus_ConcurrentUpdates_ShouldMaintainDataIntegrity() throws InterruptedException, ExecutionException {
        int userId = 1;
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Prepare different statuses to update concurrently
        String[] statuses = IntStream.range(0, threadCount)
                .mapToObj(i -> "STATUS_" + i)
                .toArray(String[]::new);

        Callable<Optional<UserResponse>>[] tasks = new Callable[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final String status = statuses[i];
            tasks[i] = () -> userService.updateStatus(userId, status);
        }

        List<Future<Optional<UserResponse>>> futures = executor.invokeAll(List.of(tasks));
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // All updates should succeed
        for (Future<Optional<UserResponse>> future : futures) {
            Optional<UserResponse> result = future.get();
            assertTrue(result.isPresent(), "Concurrent update should return present Optional");
        }

        // Final status should be one of the statuses set
        UserResponse finalUser = userService.getById(userId).orElseThrow();
        boolean statusMatch = false;
        for (String s : statuses) {
            if (s.equals(finalUser.status())) {
                statusMatch = true;
                break;
            }
        }
        assertTrue(statusMatch, "Final status should be one of the concurrently set statuses");
    }

    @Test
    public void testUpdateStatus_StatusValueValidation_FutureImprovement() {
        // This test is a placeholder for future validation logic
        // Currently, the service accepts any status value including invalid ones
        int userId = 1;
        String invalidStatus = "INVALID_STATUS_VALUE";

        Optional<UserResponse> updatedOpt = userService.updateStatus(userId, invalidStatus);
        assertTrue(updatedOpt.isPresent());
        assertEquals(invalidStatus, updatedOpt.get().status());
    }
}