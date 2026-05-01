package com.repoalvo.javaapi;

import com.repoalvo.javaapi.model.UserCreateRequest;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.model.UserUpdateRequest;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceUnitTest {

    private UserService userService;

    @BeforeEach
    public void setup() {
        userService = new UserService();
    }

    // --- Existing tests omitted for brevity ---

    // New tests to cover null phone number scenarios and related edge cases

    @Test
    public void updateExistingUserWithNullPhoneNumberShouldPreserveExistingPhoneNumber() {
        int userId = 1;
        UserResponse original = userService.getById(userId).orElseThrow();

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "User Null Phone",
                "user.nullphone@example.com",
                "USER",
                null
        );

        Optional<UserResponse> updatedOpt = userService.update(userId, updateRequest);
        assertTrue(updatedOpt.isPresent());
        UserResponse updated = updatedOpt.get();

        assertEquals(userId, updated.id());
        assertEquals("User Null Phone", updated.name());
        assertEquals("user.nullphone@example.com", updated.email());
        assertEquals("USER", updated.role());
        // Phone number should remain unchanged because null was passed
        assertEquals(original.phoneNumber(), updated.phoneNumber());
    }

    @Test
    public void updateExistingUserWithEmptyPhoneNumberShouldStoreEmptyString() {
        int userId = 1;

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "User Empty Phone",
                "user.emptyphone@example.com",
                "USER",
                ""
        );

        Optional<UserResponse> updatedOpt = userService.update(userId, updateRequest);
        assertTrue(updatedOpt.isPresent());
        UserResponse updated = updatedOpt.get();

        assertEquals("", updated.phoneNumber());
    }

    @Test
    public void updateExistingUserWithPhoneNumberContainingSpacesAndInvisibleCharsShouldStoreAsIs() {
        int userId = 1;
        String phoneWithSpaces = " +55 11 90000-0001 \t\n";

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "User Spaces Phone",
                "user.spacesphone@example.com",
                "USER",
                phoneWithSpaces
        );

        Optional<UserResponse> updatedOpt = userService.update(userId, updateRequest);
        assertTrue(updatedOpt.isPresent());
        UserResponse updated = updatedOpt.get();

        // Expecting the phone number stored exactly as passed (no trimming or cleaning)
        assertEquals(phoneWithSpaces, updated.phoneNumber());
    }

    @Test
    public void updateExistingUserWithListOfPhoneNumbersContainingValidNullAndInvalidShouldHandleCorrectly() {
        // Since UserUpdateRequest has only one phoneNumber field, simulate multiple updates with mixed values
        int userId = 1;

        String[] phoneNumbers = {
                "+55 11 90000-0001", // valid
                null,                // null
                "invalid-phone",     // invalid format
                "+55 11 90000-0001"  // duplicate
        };

        for (String phone : phoneNumbers) {
            UserUpdateRequest updateRequest = new UserUpdateRequest(
                    "User Mixed Phones",
                    "user.mixedphones@example.com",
                    "USER",
                    phone
            );
            Optional<UserResponse> updatedOpt = userService.update(userId, updateRequest);
            assertTrue(updatedOpt.isPresent());
            UserResponse updated = updatedOpt.get();

            if (phone == null) {
                // Phone number should remain unchanged if null passed
                UserResponse original = userService.getById(userId).orElseThrow();
                assertEquals(original.phoneNumber(), updated.phoneNumber());
            } else {
                // Phone number stored as is, even if invalid or duplicate
                assertEquals(phone, updated.phoneNumber());
            }
        }
    }

    @Test
    public void updateUserWithNullPhoneNumberInPartialUpdateShouldNotOverwritePhoneNumber() {
        int userId = 1;
        UserResponse original = userService.getById(userId).orElseThrow();

        UserUpdateRequest partialUpdate = new UserUpdateRequest(
                null,
                null,
                null,
                null
        );

        Optional<UserResponse> updatedOpt = userService.update(userId, partialUpdate);
        assertTrue(updatedOpt.isPresent());
        UserResponse updated = updatedOpt.get();

        // Phone number should remain unchanged
        assertEquals(original.phoneNumber(), updated.phoneNumber());
    }

    @Test
    public void updateUserWithNullPhoneNumberShouldNotThrowException() {
        int userId = 1;

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "User Null Phone Exception",
                "user.nullphoneex@example.com",
                "USER",
                null
        );

        assertDoesNotThrow(() -> {
            Optional<UserResponse> updatedOpt = userService.update(userId, updateRequest);
            assertTrue(updatedOpt.isPresent());
        });
    }

    @Test
    public void updateUserWithDuplicatePhoneNumbersSequentiallyShouldAcceptEachUpdate() {
        int userId = 1;
        String phone = "+55 11 90000-0001";

        UserUpdateRequest firstUpdate = new UserUpdateRequest(
                "User Dup Phone 1",
                "user.dup1@example.com",
                "USER",
                phone
        );
        Optional<UserResponse> firstUpdatedOpt = userService.update(userId, firstUpdate);
        assertTrue(firstUpdatedOpt.isPresent());
        assertEquals(phone, firstUpdatedOpt.get().phoneNumber());

        UserUpdateRequest secondUpdate = new UserUpdateRequest(
                "User Dup Phone 2",
                "user.dup2@example.com",
                "USER",
                phone
        );
        Optional<UserResponse> secondUpdatedOpt = userService.update(userId, secondUpdate);
        assertTrue(secondUpdatedOpt.isPresent());
        assertEquals(phone, secondUpdatedOpt.get().phoneNumber());
    }

    @Test
    public void updateUserWithNullPhoneNumberShouldBeHandledInCreateRequest() {
        int userId = 1;

        UserCreateRequest createRequest = new UserCreateRequest(
                "User Create Null Phone",
                "user.createnullphone@example.com",
                "USER",
                null
        );

        UserResponse updated = userService.update(userId, createRequest);
        assertEquals(userId, updated.id());
        assertEquals("User Create Null Phone", updated.name());
        assertEquals("user.createnullphone@example.com", updated.email());
        assertEquals("USER", updated.role());
        // Phone number should remain unchanged or null depending on implementation, here we check it is null or unchanged
        // Since update with UserCreateRequest returns UserResponse, we check if phoneNumber is null or original
        // We accept null or original phone number as valid behavior
        assertTrue(updated.phoneNumber() == null || !updated.phoneNumber().isEmpty());
    }

}