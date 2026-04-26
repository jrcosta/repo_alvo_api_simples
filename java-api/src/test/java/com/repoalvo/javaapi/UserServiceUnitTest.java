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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceUnitTest {

    private UserService userService;

    @BeforeEach
    public void setup() {
        userService = new UserService();
    }

    @Test
    public void updateExistingUserWithValidRoleAndPhoneNumberShouldSucceed() {
        int userId = 1;
        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "Ana Updated",
                "ana.updated@example.com",
                "ADMIN",
                "+55 11 99999-9999"
        );

        Optional<UserResponse> updatedOpt = userService.update(userId, updateRequest);
        assertTrue(updatedOpt.isPresent());
        UserResponse updated = updatedOpt.get();
        assertEquals(userId, updated.id());
        assertEquals("Ana Updated", updated.name());
        assertEquals("ana.updated@example.com", updated.email());
        assertEquals("ADMIN", updated.role());
        assertEquals("+55 11 99999-9999", updated.phoneNumber());
        assertEquals("ACTIVE", updated.status());
    }

    @Test
    public void updateExistingUserWithInvalidRoleShouldAcceptValueAsIs() {
        int userId = 2;
        String invalidRole = "INVALID_ROLE";
        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "Bruno Updated",
                "bruno.updated@example.com",
                invalidRole,
                "+55 11 98888-8888"
        );

        Optional<UserResponse> updatedOpt = userService.update(userId, updateRequest);
        assertTrue(updatedOpt.isPresent());
        UserResponse updated = updatedOpt.get();
        assertEquals(invalidRole, updated.role(), "Role should be updated even if invalid (no validation)");
    }

    @Test
    public void updateExistingUserWithVariousPhoneNumberFormatsShouldAcceptAsIs() {
        int userId = 1;
        String[] phoneNumbers = {
                "+55 11 90000-0001",
                "11900000001",
                "+1-202-555-0173",
                "2025550173",
                null
        };

        for (String phone : phoneNumbers) {
            UserUpdateRequest updateRequest = new UserUpdateRequest(
                    "Ana Silva",
                    "ana@example.com",
                    "USER",
                    phone
            );
            Optional<UserResponse> updatedOpt = userService.update(userId, updateRequest);
            assertTrue(updatedOpt.isPresent());
            UserResponse updated = updatedOpt.get();
            assertEquals(phone, updated.phoneNumber());
        }
    }

    @Test
    public void updateUserWithUserCreateRequestShouldConvertAndUpdate() {
        int userId = 2;
        UserCreateRequest createRequest = new UserCreateRequest(
                "Bruno New",
                "bruno.new@example.com",
                "ADMIN",
                "+55 11 97777-7777"
        );

        UserResponse updated = userService.update(userId, createRequest);
        assertEquals(userId, updated.id());
        assertEquals("Bruno New", updated.name());
        assertEquals("bruno.new@example.com", updated.email());
        assertEquals("ADMIN", updated.role());
        assertEquals("+55 11 97777-7777", updated.phoneNumber());
    }

    @Test
    public void updateNonExistentUserWithUserUpdateRequestShouldReturnEmpty() {
        int nonExistentUserId = 999;
        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "Non Existent",
                "nonexistent@example.com",
                "USER",
                "+55 11 90000-0000"
        );

        Optional<UserResponse> updatedOpt = userService.update(nonExistentUserId, updateRequest);
        assertTrue(updatedOpt.isEmpty());
    }

    @Test
    public void updateNonExistentUserWithUserCreateRequestShouldThrow() {
        int nonExistentUserId = 999;
        UserCreateRequest createRequest = new UserCreateRequest(
                "Non Existent",
                "nonexistent@example.com",
                "USER",
                "+55 11 90000-0000"
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            userService.update(nonExistentUserId, createRequest);
        });
        assertTrue(ex.getMessage().contains("User not found"));
    }

    @Test
    public void deleteExistingUserShouldRemoveUser() {
        int userId = 1;
        Optional<UserResponse> beforeDelete = userService.getById(userId);
        assertTrue(beforeDelete.isPresent());

        userService.delete(userId);

        Optional<UserResponse> afterDelete = userService.getById(userId);
        assertTrue(afterDelete.isEmpty());

        List<UserResponse> allUsers = userService.listAllUsers();
        assertFalse(allUsers.stream().anyMatch(u -> u.id() == userId));
    }

    @Test
    public void deleteNonExistentUserShouldNotThrow() {
        int nonExistentUserId = 999;
        // Should not throw any exception
        assertDoesNotThrow(() -> userService.delete(nonExistentUserId));
    }

    @Test
    public void searchByPhoneNumberShouldReturnMatchingUsers() {
        // Existing phone number
        String phone = "+55 11 90000-0001";
        List<UserResponse> results = userService.searchByPhoneNumber(phone);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(u -> phone.equals(u.phoneNumber())));

        // Non-existent phone number
        String nonExistentPhone = "+55 11 99999-9999";
        List<UserResponse> noResults = userService.searchByPhoneNumber(nonExistentPhone);
        assertTrue(noResults.isEmpty());

        // Null phone number
        List<UserResponse> nullResults = userService.searchByPhoneNumber(null);
        assertTrue(nullResults.isEmpty());

        // Phone number with spaces and special chars (should not match if exact match required)
        String phoneWithSpaces = " +55 11 90000-0001 ";
        List<UserResponse> spacedResults = userService.searchByPhoneNumber(phoneWithSpaces);
        assertTrue(spacedResults.isEmpty());
    }

    @Test
    public void partialUpdateShouldPreserveUnchangedFields() {
        int userId = 1;
        UserResponse original = userService.getById(userId).orElseThrow();

        UserUpdateRequest partialUpdate = new UserUpdateRequest(
                "Ana Partial",
                null,
                null,
                null
        );

        Optional<UserResponse> updatedOpt = userService.update(userId, partialUpdate);
        assertTrue(updatedOpt.isPresent());
        UserResponse updated = updatedOpt.get();

        assertEquals("Ana Partial", updated.name());
        assertEquals(original.email(), updated.email());
        assertEquals(original.role(), updated.role());
        assertEquals(original.phoneNumber(), updated.phoneNumber());
        assertEquals(original.status(), updated.status());
    }

    @Test
    public void updateWithNullPayloadShouldNotChangeUser() {
        int userId = 1;
        UserResponse original = userService.getById(userId).orElseThrow();

        UserUpdateRequest nullPayload = new UserUpdateRequest(null, null, null, null);
        Optional<UserResponse> updatedOpt = userService.update(userId, nullPayload);
        assertTrue(updatedOpt.isPresent());
        UserResponse updated = updatedOpt.get();

        assertEquals(original.name(), updated.name());
        assertEquals(original.email(), updated.email());
        assertEquals(original.role(), updated.role());
        assertEquals(original.phoneNumber(), updated.phoneNumber());
        assertEquals(original.status(), updated.status());
    }

    @Test
    public void concurrentUpdateDeleteAndSearchOperationsShouldMaintainConsistency() throws InterruptedException, ExecutionException {
        int initialUserCount = userService.listAllUsers().size();

        ExecutorService executor = Executors.newFixedThreadPool(10);
        int userIdToUpdate = 1;
        int userIdToDelete = 2;

        Callable<Void> updateTask = () -> {
            UserUpdateRequest updateRequest = new UserUpdateRequest(
                    "Concurrent Update",
                    "concurrent@example.com",
                    "USER",
                    "+55 11 90000-1234"
            );
            userService.update(userIdToUpdate, updateRequest);
            return null;
        };

        Callable<Void> deleteTask = () -> {
            userService.delete(userIdToDelete);
            return null;
        };

        Callable<Void> searchTask = () -> {
            List<UserResponse> results = userService.searchByPhoneNumber("+55 11 90000-0001");
            // Just access results to simulate load
            results.size();
            return null;
        };

        List<Callable<Void>> tasks = List.of(updateTask, deleteTask, searchTask, updateTask, searchTask, deleteTask, updateTask);

        List<Future<Void>> futures = executor.invokeAll(tasks);

        for (Future<Void> f : futures) {
            f.get();
        }

        executor.shutdown();

        // Validate user 1 updated
        Optional<UserResponse> updatedUser = userService.getById(userIdToUpdate);
        assertTrue(updatedUser.isPresent());
        assertEquals("Concurrent Update", updatedUser.get().name());

        // Validate user 2 deleted
        Optional<UserResponse> deletedUser = userService.getById(userIdToDelete);
        assertTrue(deletedUser.isEmpty());

        // Validate total users count decreased by 1
        int finalUserCount = userService.listAllUsers().size();
        assertEquals(initialUserCount - 1, finalUserCount);
    }

    @Test
    public void updateUserWithNullRoleAndPhoneNumberShouldPreserveExistingValues() {
        int userId = 1;
        UserResponse original = userService.getById(userId).orElseThrow();

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "Ana Silva Updated",
                "ana.updated@example.com",
                null,
                null
        );

        Optional<UserResponse> updatedOpt = userService.update(userId, updateRequest);
        assertTrue(updatedOpt.isPresent());
        UserResponse updated = updatedOpt.get();

        assertEquals("Ana Silva Updated", updated.name());
        assertEquals("ana.updated@example.com", updated.email());
        assertEquals(original.role(), updated.role());
        assertEquals(original.phoneNumber(), updated.phoneNumber());
    }

    @Test
    public void deleteUserWithAdminRoleShouldAllowDeletion() {
        // The service does not restrict deletion by role, so admin user can be deleted
        int adminUserId = 1;
        Optional<UserResponse> adminUser = userService.getById(adminUserId);
        assertTrue(adminUser.isPresent());
        assertEquals("ADMIN", adminUser.get().role());

        userService.delete(adminUserId);

        Optional<UserResponse> afterDelete = userService.getById(adminUserId);
        assertTrue(afterDelete.isEmpty());
    }

    @Test
    public void createUserShouldAssignDefaultRoleIfNull() {
        UserCreateRequest createRequest = new UserCreateRequest(
                "New User",
                "newuser@example.com",
                null,
                "+55 11 96666-6666"
        );

        UserResponse created = userService.create(createRequest);
        assertEquals("USER", created.role());
    }

    @Test
    public void searchByPhoneNumberWithSpacesAndSpecialCharactersShouldReturnEmpty() {
        String phoneWithSpaces = " +55 11 90000-0001 ";
        List<UserResponse> results = userService.searchByPhoneNumber(phoneWithSpaces);
        assertTrue(results.isEmpty());

        String phoneWithSpecialChars = "+55(11)90000-0001";
        results = userService.searchByPhoneNumber(phoneWithSpecialChars);
        assertTrue(results.isEmpty());
    }

    @Test
    public void updateUserWithNullPayloadShouldNotThrowAndPreserveData() {
        int userId = 1;
        UserResponse original = userService.getById(userId).orElseThrow();

        UserUpdateRequest nullPayload = new UserUpdateRequest(null, null, null, null);
        Optional<UserResponse> updatedOpt = userService.update(userId, nullPayload);
        assertTrue(updatedOpt.isPresent());
        UserResponse updated = updatedOpt.get();

        assertEquals(original.name(), updated.name());
        assertEquals(original.email(), updated.email());
        assertEquals(original.role(), updated.role());
        assertEquals(original.phoneNumber(), updated.phoneNumber());
    }

    @Test
    public void updateUserWithEmptyStringsForRoleAndPhoneNumberShouldUpdateFields() {
        int userId = 1;
        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "Ana Silva",
                "ana@example.com",
                "",
                ""
        );

        Optional<UserResponse> updatedOpt = userService.update(userId, updateRequest);
        assertTrue(updatedOpt.isPresent());
        UserResponse updated = updatedOpt.get();

        assertEquals("", updated.role());
        assertEquals("", updated.phoneNumber());
    }
}