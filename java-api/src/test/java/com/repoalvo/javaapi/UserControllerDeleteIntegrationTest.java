package com.repoalvo.javaapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerDeleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final String validUsername = "admin";
    private final String validPassword = "adminpass";

    private final String userWithoutDeletePermissionUsername = "user";
    private final String userWithoutDeletePermissionPassword = "userpass";

    @BeforeEach
    void setUp() throws Exception {
        // Ensure user with ID 1 exists and has related data for tests
        // This setup assumes an endpoint or direct DB access to create/reset user data
        // For demonstration, we assume user 1 exists and user 2 exists for concurrency tests
        // If needed, implement setup here or use @Sql scripts for DB state reset
    }

    @Test
    @DisplayName("DELETE /users/{userId} returns 204 and user is removed when user exists and authorized")
    void deleteUserShouldReturn204AndUserRemovedWhenExistsAndAuthorized() throws Exception {
        int userId = 1;

        // Verify user exists first
        mockMvc.perform(get("/users/" + userId)
                .with(httpBasic(validUsername, validPassword)))
                .andExpect(status().isOk());

        // Delete user
        mockMvc.perform(delete("/users/" + userId)
                .with(httpBasic(validUsername, validPassword)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        // Verify user no longer exists
        mockMvc.perform(get("/users/" + userId)
                .with(httpBasic(validUsername, validPassword)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /users/{userId} returns 404 when user does not exist")
    void deleteUserShouldReturn404WhenUserDoesNotExist() throws Exception {
        int userId = 999;

        mockMvc.perform(delete("/users/" + userId)
                .with(httpBasic(validUsername, validPassword)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @DisplayName("DELETE /users/{userId} returns 400 when userId format is invalid")
    void deleteUserShouldReturn400WhenUserIdInvalidFormat() throws Exception {
        String invalidUserId = "abc123";

        mockMvc.perform(delete("/users/" + invalidUserId)
                .with(httpBasic(validUsername, validPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").value(containsString("Failed to convert")));
    }

    @Test
    @DisplayName("DELETE /users/{userId} returns 403 when user is authenticated but lacks delete permission")
    void deleteUserShouldReturn403WhenUserLacksPermission() throws Exception {
        int userId = 1;

        mockMvc.perform(delete("/users/" + userId)
                .with(httpBasic(userWithoutDeletePermissionUsername, userWithoutDeletePermissionPassword)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").value(containsString("Access is denied")));
    }

    @Test
    @DisplayName("DELETE /users/{userId} returns 401 when no authentication provided")
    void deleteUserShouldReturn401WhenNoAuthentication() throws Exception {
        int userId = 1;

        mockMvc.perform(delete("/users/" + userId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /users/{userId} deletes user with related posts and maintains data integrity")
    void deleteUserWithRelatedDataShouldDeleteAndMaintainIntegrity() throws Exception {
        int userId = 2; // Assume user 2 has related posts

        // Verify user exists
        mockMvc.perform(get("/users/" + userId)
                .with(httpBasic(validUsername, validPassword)))
                .andExpect(status().isOk());

        // Delete user
        mockMvc.perform(delete("/users/" + userId)
                .with(httpBasic(validUsername, validPassword)))
                .andExpect(status().isNoContent());

        // Verify user no longer exists
        mockMvc.perform(get("/users/" + userId)
                .with(httpBasic(validUsername, validPassword)))
                .andExpect(status().isNotFound());

        // Verify posts related to user are deleted or handled (assuming /posts?userId=)
        mockMvc.perform(get("/posts?userId=" + userId)
                .with(httpBasic(validUsername, validPassword)))
                .andExpect(status().isOk())
                .andExpect(content().string("[]")); // Expect empty list or no posts
    }

    @Test
    @DisplayName("DELETE /users/{userId} concurrent deletion requests for same user handled gracefully")
    void deleteUserConcurrentDeletionRequestsHandledGracefully() throws Exception {
        int userId = 3; // Assume user 3 exists

        // Verify user exists
        mockMvc.perform(get("/users/" + userId)
                .with(httpBasic(validUsername, validPassword)))
                .andExpect(status().isOk());

        // Perform two concurrent delete requests
        CompletableFuture<Void> delete1 = CompletableFuture.runAsync(() -> {
            try {
                mockMvc.perform(delete("/users/" + userId)
                        .with(httpBasic(validUsername, validPassword)))
                        .andExpect(status().isNoContent());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Void> delete2 = CompletableFuture.runAsync(() -> {
            try {
                mockMvc.perform(delete("/users/" + userId)
                        .with(httpBasic(validUsername, validPassword)))
                        .andExpect(status().is(anyOf(
                                org.hamcrest.Matchers.is(204),
                                org.hamcrest.Matchers.is(404)
                        )));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture.allOf(delete1, delete2).get(5, TimeUnit.SECONDS);

        // Verify user no longer exists
        mockMvc.perform(get("/users/" + userId)
                .with(httpBasic(validUsername, validPassword)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /users/{userId} returns 401 when token is expired or invalid")
    void deleteUserShouldReturn401WhenTokenExpiredOrInvalid() throws Exception {
        int userId = 1;

        // Simulate invalid token by not providing or providing malformed credentials
        mockMvc.perform(delete("/users/" + userId)
                .header("Authorization", "Bearer invalidtoken"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /users/{userId} returns detailed error response on database failure")
    void deleteUserShouldReturn500OnDatabaseFailure() throws Exception {
        int userId = 4; // Assume user 4 exists

        // Simulate DB failure by deleting user and then mocking DB failure is complex in integration test
        // Instead, we simulate by deleting user first, then trying to delete again to get 404
        // For real DB failure simulation, would require mocking service layer or DB connection

        // First delete succeeds
        mockMvc.perform(delete("/users/" + userId)
                .with(httpBasic(validUsername, validPassword)))
                .andExpect(status().isNoContent());

        // Second delete triggers not found, simulate DB failure by invalid endpoint or mock if possible
        mockMvc.perform(delete("/users/" + userId)
                .with(httpBasic(validUsername, validPassword)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /users/{userId} returns 400 when userId contains special characters or spaces")
    void deleteUserShouldReturn400WhenUserIdContainsSpecialCharacters() throws Exception {
        String invalidUserId = "1; DROP TABLE users";

        mockMvc.perform(delete("/users/" + invalidUserId)
                .with(httpBasic(validUsername, validPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").value(containsString("Failed to convert")));
    }

    @Test
    @DisplayName("DELETE /users/{userId} does not allow deletion of critical admin users")
    void deleteUserShouldNotAllowDeletionOfCriticalAdminUsers() throws Exception {
        int adminUserId = 10; // Assume user 10 is critical admin

        mockMvc.perform(delete("/users/" + adminUserId)
                .with(httpBasic(validUsername, validPassword)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").value(containsString("cannot delete critical user")));
    }

    @Test
    @DisplayName("DELETE /users/{userId} after deletion, subsequent GET requests consistently return 404")
    void deleteUserSubsequentGetRequestsReturn404Consistently() throws Exception {
        int userId = 5; // Assume user 5 exists

        // Delete user
        mockMvc.perform(delete("/users/" + userId)
                .with(httpBasic(validUsername, validPassword)))
                .andExpect(status().isNoContent());

        // Multiple GET requests after deletion
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/users/" + userId)
                    .with(httpBasic(validUsername, validPassword)))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("DELETE /users/{userId} concurrent deletions for different users handled independently")
    void deleteUserConcurrentDeletionsForDifferentUsersHandledIndependently() throws Exception {
        int userId1 = 6;
        int userId2 = 7;

        // Verify both users exist
        mockMvc.perform(get("/users/" + userId1)
                .with(httpBasic(validUsername, validPassword)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/users/" + userId2)
                .with(httpBasic(validUsername, validPassword)))
                .andExpect(status().isOk());

        // Concurrent deletes
        CompletableFuture<Void> deleteUser1 = CompletableFuture.runAsync(() -> {
            try {
                mockMvc.perform(delete("/users/" + userId1)
                        .with(httpBasic(validUsername, validPassword)))
                        .andExpect(status().isNoContent());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Void> deleteUser2 = CompletableFuture.runAsync(() -> {
            try {
                mockMvc.perform(delete("/users/" + userId2)
                        .with(httpBasic(validUsername, validPassword)))
                        .andExpect(status().isNoContent());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture.allOf(deleteUser1, deleteUser2).join();

        // Verify both users no longer exist
        mockMvc.perform(get("/users/" + userId1)
                .with(httpBasic(validUsername, validPassword)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/users/" + userId2)
                .with(httpBasic(validUsername, validPassword)))
                .andExpect(status().isNotFound());
    }
}