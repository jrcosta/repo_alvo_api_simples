package com.repoalvo.javaapi;

import com.repoalvo.javaapi.model.UserCreateRequest;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerDeleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @BeforeEach
    void setup() {
        userService.reset();
    }

    @Test
    @DisplayName("DELETE /users/{userId} returns 204 when user exists with valid admin authentication")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteUserShouldReturn204WhenUserExistsWithAdminAuth() throws Exception {
        // Ana (id=1) é ADMIN e tem vip=true — não pode ser deletada
        // Criar um usuário USER para deletar
        UserCreateRequest req = new UserCreateRequest("Test User", "test.delete@example.com", "USER", null);
        UserResponse created = userService.create(req);

        mockMvc.perform(get("/users/" + created.id()))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/users/" + created.id()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/" + created.id()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /users/{userId} returns 401 Unauthorized when no authentication provided")
    void deleteUserShouldReturn401WhenNoAuth() throws Exception {
        mockMvc.perform(delete("/users/2"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /users/{userId} returns 403 Forbidden when authenticated user lacks admin role")
    @WithMockUser(username = "user", roles = {"USER"})
    void deleteUserShouldReturn403WhenUserNotAdmin() throws Exception {
        mockMvc.perform(delete("/users/2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /users/{userId} returns 400 Bad Request for invalid userId formats")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteUserShouldReturn400ForInvalidUserIdFormats() throws Exception {
        // Strings não numéricas retornam 400 via MethodArgumentTypeMismatchException
        String[] invalidUserIds = {"abc", "123abc", "null"};

        for (String invalidId : invalidUserIds) {
            mockMvc.perform(delete("/users/" + invalidId))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("Invalid userId")));
        }

        // Valores numéricos inválidos retornam 400 via deleteAtomic (userId < 1)
        mockMvc.perform(delete("/users/-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/users/0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /users/{userId} returns 404 Not Found when user does not exist")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteUserShouldReturn404WhenUserDoesNotExistWithAdminAuth() throws Exception {
        mockMvc.perform(delete("/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /users/{userId} prevents deletion of critical admin users (vip=true)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteUserShouldPreventDeletionOfCriticalAdminUsers() throws Exception {
        // Ana (id=1) é ADMIN → vip=true → não pode ser deletada
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Cannot delete critical admin user")));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /users/{userId} deletes user and maintains data integrity")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteUserWithRelatedDataShouldMaintainIntegrity() throws Exception {
        UserCreateRequest req = new UserCreateRequest("User With Posts", "user.posts@example.com", "USER", null);
        UserResponse createdUser = userService.create(req);

        mockMvc.perform(delete("/users/" + createdUser.id()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/" + createdUser.id()))
                .andExpect(status().isNotFound());

        List<String> posts = userService.getPostsByUserId(createdUser.id());
        assert(posts.isEmpty());
    }

    @Test
    @DisplayName("DELETE /users/{userId} handles concurrent deletions of the same user gracefully")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteUserConcurrentlySameUser() throws Exception {
        UserCreateRequest user = new UserCreateRequest("Concurrent User", "concurrent.user@example.com", "USER", null);
        UserResponse createdUser = userService.create(user);
        int userId = createdUser.id();

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<Integer>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                try {
                    return mockMvc.perform(
                            delete("/users/" + userId)
                                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                } catch (Exception e) {
                    return 500;
                }
            });
        }

        List<Future<Integer>> results = executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> future : results) {
            statuses.add(future.get());
        }

        long noContentCount = statuses.stream().filter(s -> s == 204).count();
        long notFoundCount = statuses.stream().filter(s -> s == 404).count();

        assert(noContentCount == 1);
        assert(notFoundCount == threadCount - 1);
    }

    @Test
    @DisplayName("DELETE /users/{userId} handles concurrent deletions of different users correctly")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteUserConcurrentlyDifferentUsers() throws Exception {
        int userCount = 5;
        List<UserResponse> users = new ArrayList<>();
        for (int i = 0; i < userCount; i++) {
            UserCreateRequest user = new UserCreateRequest("User " + i, "concurrent" + i + "@example.com", "USER", null);
            users.add(userService.create(user));
        }

        ExecutorService executor = Executors.newFixedThreadPool(userCount);
        List<Callable<Integer>> tasks = new ArrayList<>();

        for (UserResponse user : users) {
            tasks.add(() -> {
                try {
                    return mockMvc.perform(
                            delete("/users/" + user.id())
                                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                } catch (Exception e) {
                    return 500;
                }
            });
        }

        List<Future<Integer>> results = executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        for (Future<Integer> future : results) {
            assert(future.get() == 204);
        }

        for (UserResponse user : users) {
            mockMvc.perform(get("/users/" + user.id()))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("DELETE /users/{userId} returns 500 on simulated database failure (fallback: normal delete)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteUserShouldReturn500OnDatabaseFailure() throws Exception {
        UserCreateRequest user = new UserCreateRequest("Fail User", "fail.user@example.com", "USER", null);
        UserResponse createdUser = userService.create(user);

        mockMvc.perform(delete("/users/" + createdUser.id()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /users/{userId} returns 400 when userId is null string or missing")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteUserShouldReturn400WhenUserIdIsNullOrMissing() throws Exception {
        mockMvc.perform(delete("/users/"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/users/null"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid userId")));
    }

    @Test
    @DisplayName("DELETE /users/{userId} returns 403 Forbidden for non-admin users")
    @WithMockUser(username = "blockedUser", roles = {"USER"})
    void deleteUserShouldReturn403ForBlockedUserAttemptingDeletion() throws Exception {
        mockMvc.perform(delete("/users/2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /users/{userId} logs audit entry on successful deletion")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteUserShouldLogAuditOnSuccess() throws Exception {
        // Bruno (id=2) é USER → pode ser deletado
        mockMvc.perform(delete("/users/2"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/2"))
                .andExpect(status().isNotFound());
    }
}
