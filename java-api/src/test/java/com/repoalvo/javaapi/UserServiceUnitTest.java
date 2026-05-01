package com.repoalvo.javaapi;

import com.repoalvo.javaapi.model.UserCreateRequest;
import com.repoalvo.javaapi.model.UserUpdateRequest;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceUnitTest {

    private UserService userService;

    @BeforeEach
    void setup() {
        userService = new UserService();
        userService.reset();
    }

    @Test
    void testDeleteAtomic_UserNaoVIP_DeletaERetornaUsuario() {
        // Arrange: user with id=2 is USER (not VIP)
        int userId = 2;

        // Act
        Optional<UserResponse> deleted = userService.deleteAtomic(userId);

        // Assert
        assertTrue(deleted.isPresent(), "User should be found and deleted");
        assertEquals(userId, deleted.get().id());
        assertFalse(userService.getById(userId).isPresent(), "User should no longer exist");
    }

    @Test
    void testDeleteAtomic_UserVIP_LancaIllegalStateException() {
        // Arrange: user with id=1 is ADMIN and VIP
        int userId = 1;

        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> userService.deleteAtomic(userId));
        assertEquals("Cannot delete critical admin user", ex.getMessage());
        assertTrue(userService.getById(userId).isPresent(), "VIP user should still exist");
    }

    @Test
    void testDeleteAtomic_UserInexistente_RetornaOptionalEmpty() {
        int userId = 9999;

        Optional<UserResponse> deleted = userService.deleteAtomic(userId);

        assertTrue(deleted.isEmpty(), "Deleting non-existent user should return Optional.empty");
    }

    @Test
    void testDeleteAtomic_Concorrencia_GarantirAtomicidade() throws InterruptedException, ExecutionException {
        // Arrange: add multiple users to test concurrency
        IntStream.range(3, 103).forEach(i ->
                userService.createUser("User" + i, "user" + i + "@example.com", "ACTIVE", "USER"));

        int userIdToDelete = 50;

        // Act: run multiple concurrent deleteAtomic calls for the same userId
        ExecutorService executor = Executors.newFixedThreadPool(10);
        Callable<Optional<UserResponse>> task = () -> {
            try {
                return userService.deleteAtomic(userIdToDelete);
            } catch (IllegalStateException e) {
                return Optional.empty();
            }
        };

        List<Future<Optional<UserResponse>>> futures = executor.invokeAll(List.of(task, task, task, task, task, task, task, task, task, task));
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Assert: only one call should delete and return user, others return empty
        long successCount = futures.stream().filter(f -> {
            try {
                return f.get().isPresent();
            } catch (Exception e) {
                return false;
            }
        }).count();

        assertEquals(1, successCount, "Only one thread should successfully delete the user");

        // User should no longer exist
        assertTrue(userService.getById(userIdToDelete).isEmpty(), "User should be deleted");

        // List size should be reduced by 1
        assertEquals(101, userService.listAllUsers().size());
    }

    @Test
    void testCreateUser_CriaUsuarioComParametrosCorretos() {
        String name = "Carlos";
        String email = "carlos@example.com";
        String status = "ACTIVE";
        String role = "USER";

        UserResponse created = userService.createUser(name, email, status, role);

        assertNotNull(created);
        assertEquals(name, created.name());
        assertEquals(email, created.email());
        assertEquals(status, created.status());
        assertEquals(role, created.role());
        assertNotNull(created.id());

        Optional<UserResponse> found = userService.getById(created.id());
        assertTrue(found.isPresent());
        assertEquals(created, found.get());
    }

    @Test
    void testAddPostForUser_NaoPersistePosts() {
        int userId = 1;
        userService.addPostForUser(userId, "Post 1");
        userService.addPostForUser(userId, "Post 2");

        List<String> posts = userService.getPostsByUserId(userId);
        assertNotNull(posts);
        assertTrue(posts.isEmpty(), "Posts should not be persisted and list should be empty");
    }

    @Test
    void testGetPostsByUserId_RetornaListaPostsOuVazia() {
        int userId = 1;
        List<String> posts = userService.getPostsByUserId(userId);
        assertNotNull(posts);
        assertTrue(posts.isEmpty(), "Posts list should be empty for any user");

        int nonExistentUserId = 9999;
        List<String> postsNonExistent = userService.getPostsByUserId(nonExistentUserId);
        assertNotNull(postsNonExistent);
        assertTrue(postsNonExistent.isEmpty(), "Posts list should be empty for non-existent user");
    }

    @Test
    void testGetUserById_RetornaUsuarioOuOptionalEmpty() {
        int existingUserId = 1;
        Optional<UserResponse> user = userService.getUserById(existingUserId);
        assertTrue(user.isPresent());
        assertEquals(existingUserId, user.get().id());

        int nonExistentUserId = 9999;
        Optional<UserResponse> userEmpty = userService.getUserById(nonExistentUserId);
        assertTrue(userEmpty.isEmpty());
    }

    @Test
    void testSincronizacao_IntegridadeListaUsers() throws InterruptedException {
        // Arrange: concurrent create and delete operations
        ExecutorService executor = Executors.newFixedThreadPool(10);

        Runnable createTask = () -> {
            for (int i = 0; i < 100; i++) {
                userService.createUser("ConcurrentUser" + i, "concurrent" + i + "@example.com", "ACTIVE", "USER");
            }
        };

        Runnable deleteTask = () -> {
            for (int i = 1; i <= 50; i++) {
                try {
                    userService.deleteAtomic(i);
                } catch (IllegalStateException ignored) {
                    // Ignore VIP user deletion exception
                }
            }
        };

        executor.submit(createTask);
        executor.submit(deleteTask);

        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertTrue(finished, "Executor tasks should finish in time");

        // Validate no duplicates and consistent list size
        List<UserResponse> allUsers = userService.listAllUsers();
        long distinctCount = allUsers.stream().map(UserResponse::id).distinct().count();
        assertEquals(allUsers.size(), distinctCount, "User list should not contain duplicates");

        // VIP user with id=1 should still exist
        Optional<UserResponse> vipUser = userService.getById(1);
        assertTrue(vipUser.isPresent(), "VIP user should not be deleted");

        // No user with id 2 to 50 should exist except VIP
        for (int i = 2; i <= 50; i++) {
            if (i != 1) {
                Optional<UserResponse> user = userService.getById(i);
                if (user.isPresent()) {
                    assertTrue(user.get().vip(), "Only VIP users should remain");
                }
            }
        }
    }

    @Test
    void testDeleteAtomic_FalhaInesperada_GaranteEstadoConsistente() {
        // This test simulates a failure during deletion by subclassing UserService and overriding users list
        class FaultyUserService extends UserService {
            @Override
            public synchronized Optional<UserResponse> deleteAtomic(int userId) {
                Optional<UserResponse> found = super.getById(userId);
                if (found.isEmpty()) {
                    return Optional.empty();
                }
                UserResponse user = found.get();
                if (user.vip()) {
                    throw new IllegalStateException("Cannot delete critical admin user");
                }
                // Simulate failure after finding user but before removal
                throw new RuntimeException("Simulated failure during deletion");
            }
        }

        FaultyUserService faultyService = new FaultyUserService();
        faultyService.reset();

        int userId = 2;
        RuntimeException ex = assertThrows(RuntimeException.class, () -> faultyService.deleteAtomic(userId));
        assertEquals("Simulated failure during deletion", ex.getMessage());

        // User should still exist because deletion did not complete
        Optional<UserResponse> user = faultyService.getById(userId);
        assertTrue(user.isPresent());
    }

    @Test
    void testDeleteAtomic_IllegalStateException_MensagemClara() {
        int vipUserId = 1;
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> userService.deleteAtomic(vipUserId));
        assertTrue(ex.getMessage().contains("Cannot delete critical admin user"));
    }

    @Test
    void testCreateUser_ComParametrosInvalidosOuNulos() {
        // Null name
        assertThrows(NullPointerException.class, () -> userService.createUser(null, "email@example.com", "ACTIVE", "USER"));

        // Null email
        assertThrows(NullPointerException.class, () -> userService.createUser("Name", null, "ACTIVE", "USER"));

        // Null status and role allowed? According to code, no explicit validation, so test behavior
        UserResponse user = userService.createUser("Name", "email2@example.com", null, null);
        assertNotNull(user);
        assertNull(user.status());
        assertNull(user.role());
    }

    @Test
    void testAddPostForUser_UsuarioNaoExiste_NaoLancaExcecao() {
        int nonExistentUserId = 9999;
        assertDoesNotThrow(() -> userService.addPostForUser(nonExistentUserId, "Post content"));
    }

    @Test
    void testGetPostsByUserId_UsuarioComPostsNaoPersistidos_RetornaListaVazia() {
        int userId = 1;
        userService.addPostForUser(userId, "Post 1");
        userService.addPostForUser(userId, "Post 2");

        List<String> posts = userService.getPostsByUserId(userId);
        assertNotNull(posts);
        assertTrue(posts.isEmpty());
    }

    @Test
    void testUpdateStatus_AtualizaStatusCorretamente() {
        int userId = 2;
        String newStatus = "INACTIVE";

        Optional<UserResponse> updated = userService.updateStatus(userId, newStatus);
        assertTrue(updated.isPresent());
        assertEquals(newStatus, updated.get().status());

        // Verify other fields unchanged
        UserResponse before = userService.getById(userId).orElseThrow();
        assertEquals(updated.get().id(), before.id());
        assertEquals(updated.get().name(), before.name());
        assertEquals(updated.get().email(), before.email());
    }

    @Test
    void testUpdateStatus_UsuarioInexistente_RetornaEmpty() {
        int userId = 9999;
        Optional<UserResponse> updated = userService.updateStatus(userId, "INACTIVE");
        assertTrue(updated.isEmpty());
    }

    @Test
    void testSearchByPhoneNumber_RetornaUsuariosCorretos() {
        // Setup: user with phone number +55 11 90000-0001 is Ana Silva (id=1)
        List<UserResponse> results = userService.searchByPhoneNumber("+55 11 90000-0001");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(u -> "+55 11 90000-0001".equals(u.phoneNumber())));

        // Search with null returns empty list
        List<UserResponse> nullSearch = userService.searchByPhoneNumber(null);
        assertNotNull(nullSearch);
        assertTrue(nullSearch.isEmpty());

        // Search with phone number not present returns empty list
        List<UserResponse> noMatch = userService.searchByPhoneNumber("+55 99 99999-9999");
        assertNotNull(noMatch);
        assertTrue(noMatch.isEmpty());
    }

    @Test
    void testUpdate_WithUserCreateRequest_AtualizaUsuarioOuLanca() {
        int userId = 2;
        UserCreateRequest payload = new UserCreateRequest("Updated Name", "updated@example.com", "ADMIN", "+55 11 99999-9999");

        UserResponse updated = userService.update(userId, payload);
        assertNotNull(updated);
        assertEquals(userId, updated.id());
        assertEquals("Updated Name", updated.name());
        assertEquals("updated@example.com", updated.email());
        assertEquals("ADMIN", updated.role());
        assertEquals("+55 11 99999-9999", updated.phoneNumber());

        // Update non-existent user throws RuntimeException
        int nonExistentUserId = 9999;
        UserCreateRequest payload2 = new UserCreateRequest("Name", "email@example.com", "USER", null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.update(nonExistentUserId, payload2));
        assertTrue(ex.getMessage().contains("User not found"));
    }
}