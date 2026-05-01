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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
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
        // Garantir estado consistente para usuários fixos 1 e 2
        // Criar usuário vip=true id=1 se não existir
        if (!userService.existsById(1)) {
            UserCreateRequest adminReq = new UserCreateRequest("Ana Admin", "ana.admin@example.com", "ADMIN", true);
            UserResponse adminUser = userService.createWithId(1, adminReq);
            assertNotNull(adminUser);
            assertTrue(adminUser.vip());
        }
        // Criar usuário deletável id=2 se não existir
        if (!userService.existsById(2)) {
            UserCreateRequest userReq = new UserCreateRequest("Bob User", "bob.user@example.com", "USER", false);
            UserResponse user = userService.createWithId(2, userReq);
            assertNotNull(user);
            assertFalse(user.vip());
        }
    }

    @Test
    @DisplayName("testCreateUserForDeletion_Success: criação dinâmica de usuário com vip=false e papel correto")
    void testCreateUserForDeletion_Success() {
        UserCreateRequest req = new UserCreateRequest("Dynamic User", "dynamic.user@example.com", "USER", null);
        UserResponse created = userService.create(req);
        assertNotNull(created, "Usuário criado dinamicamente não pode ser nulo");
        assertTrue(created.id() > 0, "ID do usuário criado deve ser maior que zero");
        assertEquals("Dynamic User", created.name());
        assertEquals("dynamic.user@example.com", created.email());
        assertFalse(created.vip(), "Usuário criado deve ter vip=false");
        assertTrue(created.roles().contains("USER"), "Usuário criado deve conter papel USER");
    }

    @Test
    @DisplayName("testCreateUserForDeletion_NoSilentFailure: criação dinâmica não deve falhar silenciosamente")
    void testCreateUserForDeletion_NoSilentFailure() {
        UserCreateRequest req = new UserCreateRequest("Silent Fail User", "silent.fail@example.com", "USER", null);
        UserResponse created = null;
        try {
            created = userService.create(req);
        } catch (Exception e) {
            fail("Criação dinâmica de usuário falhou com exceção: " + e.getMessage());
        }
        assertNotNull(created, "Usuário criado dinamicamente não pode ser nulo");
        assertTrue(created.id() > 0, "ID do usuário criado deve ser maior que zero");
    }

    @Test
    @DisplayName("testDeleteUser_VipUser_NotAllowed: não permite deleção de usuário vip=true")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDeleteUser_VipUser_NotAllowed() throws Exception {
        // Usuário vip=true fixo (Ana id=1)
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Cannot delete critical admin user")));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("testDeleteUser_InvalidUserIdFormat_NonNumeric: retorna 400 para userId não numérico")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDeleteUser_InvalidUserIdFormat_NonNumeric() throws Exception {
        String[] invalidUserIds = {"abc", "123abc", "null", " ", "user!@#", "9999999999999999999999999999999999999999"};
        for (String invalidId : invalidUserIds) {
            mockMvc.perform(delete("/users/" + invalidId)
                    .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("Invalid userId")));
        }
    }

    @Test
    @DisplayName("testDeleteUser_InvalidUserIdFormat_NegativeOrZero: retorna 400 para userId negativo ou zero")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDeleteUser_InvalidUserIdFormat_NegativeOrZero() throws Exception {
        mockMvc.perform(delete("/users/-1")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid userId")));
        mockMvc.perform(delete("/users/0")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid userId")));
    }

    @Test
    @DisplayName("testDeleteUser_Unauthenticated_And_Unauthorized: retorna 401 sem autenticação e 403 sem papel ADMIN")
    void testDeleteUser_Unauthenticated_And_Unauthorized() throws Exception {
        // Sem autenticação
        mockMvc.perform(delete("/users/2"))
                .andExpect(status().isUnauthorized());

        // Com autenticação mas sem papel ADMIN
        mockMvc.perform(delete("/users/2")
                .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("testDeleteUser_Success: deleção de usuário criado dinamicamente e integridade dos dados relacionados")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDeleteUser_Success() throws Exception {
        UserCreateRequest req = new UserCreateRequest("User To Delete", "user.delete@example.com", "USER", null);
        UserResponse createdUser = userService.create(req);
        assertNotNull(createdUser, "Usuário criado para deleção não pode ser nulo");

        // Criar posts relacionados para verificar integridade
        userService.createPostForUser(createdUser.id(), "Post 1");
        userService.createPostForUser(createdUser.id(), "Post 2");

        mockMvc.perform(delete("/users/" + createdUser.id())
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/" + createdUser.id())
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());

        List<String> posts = userService.getPostsByUserId(createdUser.id());
        assertTrue(posts.isEmpty(), "Posts relacionados devem ser removidos após deleção do usuário");
    }

    @Test
    @DisplayName("testDeleteUser_Concurrency_SameUser: concorrência na deleção do mesmo usuário retorna 204 e 404")
    void testDeleteUser_Concurrency_SameUser() throws Exception {
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

        assertEquals(1, noContentCount, "Apenas uma requisição deve retornar 204 No Content");
        assertEquals(threadCount - 1, notFoundCount, "Demais requisições devem retornar 404 Not Found");
    }

    @Test
    @DisplayName("testDeleteUser_Concurrency_DifferentUsers: concorrência na deleção de múltiplos usuários retorna 204 para todos")
    void testDeleteUser_Concurrency_DifferentUsers() throws Exception {
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
            assertEquals(204, future.get(), "Todas as deleções concorrentes devem retornar 204 No Content");
        }

        for (UserResponse user : users) {
            mockMvc.perform(get("/users/" + user.id())
                    .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("testDeleteUser_AuditLogEntry: valida que deleção gera entrada de auditoria com conteúdo e timestamp")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDeleteUser_AuditLogEntry() throws Exception {
        UserCreateRequest req = new UserCreateRequest("Audit User", "audit.user@example.com", "USER", null);
        UserResponse createdUser = userService.create(req);
        assertNotNull(createdUser, "Usuário criado para auditoria não pode ser nulo");

        Instant beforeDeletion = Instant.now();

        mockMvc.perform(delete("/users/" + createdUser.id())
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());

        // Verificar que a auditoria foi registrada
        boolean auditExists = userService.auditLogContainsEntryForUserDeletion(createdUser.id());
        assertTrue(auditExists, "Deleção deve gerar entrada de auditoria");

        // Verificar detalhes da auditoria
        var auditEntry = userService.getAuditLogEntryForUserDeletion(createdUser.id());
        assertNotNull(auditEntry, "Entrada de auditoria não pode ser nula");
        assertEquals(createdUser.id(), auditEntry.getUserId(), "Audit log userId deve corresponder");
        assertTrue(auditEntry.getTimestamp().isAfter(beforeDeletion) || auditEntry.getTimestamp().equals(beforeDeletion),
                "Timestamp da auditoria deve ser após ou igual ao momento da deleção");
        assertTrue(auditEntry.getAction().contains("delete"), "Ação da auditoria deve indicar deleção");
    }

    @Test
    @DisplayName("testDeleteUser_NullOrEmptyUserId: retorna 400 ou 404 para userId nulo, vazio ou ausente")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDeleteUser_NullOrEmptyUserId() throws Exception {
        mockMvc.perform(delete("/users/"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/users/null")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid userId")));

        mockMvc.perform(delete("/users/ ")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid userId")));
    }

    @Test
    @DisplayName("testDeleteUser_FixedUserIds_Consistency: usuários fixos 1 e 2 estão consistentes para testes")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDeleteUser_FixedUserIds_Consistency() throws Exception {
        // Usuário 1 é vip=true e não pode ser deletado
        mockMvc.perform(get("/users/1")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/users/1")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());

        // Usuário 2 é deletável
        mockMvc.perform(get("/users/2")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/users/2")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/2")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("testDeleteUser_FailureSimulation: simula falha no banco e valida fallback para erro 500")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDeleteUser_FailureSimulation() throws Exception {
        // Criar usuário para deleção
        UserCreateRequest user = new UserCreateRequest("FailSim User", "failsim.user@example.com", "USER", null);
        UserResponse createdUser = userService.create(user);
        assertNotNull(createdUser, "Usuário criado para simulação de falha não pode ser nulo");

        // Simular falha no banco para deleção
        userService.simulateFailureOnNextDelete(true);

        mockMvc.perform(delete("/users/" + createdUser.id())
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isInternalServerError());

        // Remover simulação e tentar deleção normal
        userService.simulateFailureOnNextDelete(false);

        mockMvc.perform(delete("/users/" + createdUser.id())
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/" + createdUser.id())
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("testDeleteUser_AuthenticationContextConsistency: autenticação explícita não altera comportamento")
    void testDeleteUser_AuthenticationContextConsistency() throws Exception {
        UserCreateRequest user = new UserCreateRequest("AuthContext User", "authcontext.user@example.com", "USER", null);
        UserResponse createdUser = userService.create(user);
        assertNotNull(createdUser, "Usuário criado para teste de contexto de autenticação não pode ser nulo");

        // Deleção com autenticação explícita via SecurityMockMvcRequestPostProcessors
        mockMvc.perform(delete("/users/" + createdUser.id())
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/" + createdUser.id())
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("testDeleteUser_InvalidUserIdFormat_Additional: valida formatos inválidos adicionais para userId")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDeleteUser_InvalidUserIdFormat_Additional() throws Exception {
        String[] invalidUserIds = {"   ", "\t", "\n", "user@name", "123 456", "0000000000000000000000000000000000000000"};
        for (String invalidId : invalidUserIds) {
            mockMvc.perform(delete("/users/" + invalidId)
                    .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("Invalid userId")));
        }
    }

    @Test
    @DisplayName("testDeleteUser_Concurrency_DifferentAuths_SameUser: concorrência com autenticações diferentes no mesmo usuário")
    void testDeleteUser_Concurrency_DifferentAuths_SameUser() throws Exception {
        UserCreateRequest user = new UserCreateRequest("Concurrent Auth User", "concurrent.auth.user@example.com", "USER", null);
        UserResponse createdUser = userService.create(user);
        int userId = createdUser.id();

        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<Integer>> tasks = new ArrayList<>();

        String[] admins = {"admin1", "admin2", "admin3"};

        for (int i = 0; i < threadCount; i++) {
            final String adminName = admins[i];
            tasks.add(() -> {
                try {
                    return mockMvc.perform(
                            delete("/users/" + userId)
                                    .with(SecurityMockMvcRequestPostProcessors.user(adminName).roles("ADMIN")))
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

        assertEquals(1, noContentCount, "Apenas uma requisição deve retornar 204 No Content");
        assertEquals(threadCount - 1, notFoundCount, "Demais requisições devem retornar 404 Not Found");
    }

    @Test
    @DisplayName("testDeleteUser_WithDifferentRoles: deleção respeita regras de autorização para papéis diferentes")
    void testDeleteUser_WithDifferentRoles() throws Exception {
        UserCreateRequest user = new UserCreateRequest("Role Test User", "role.test.user@example.com", "USER", null);
        UserResponse createdUser = userService.create(user);
        int userId = createdUser.id();

        // Usuário com papel USER não pode deletar
        mockMvc.perform(delete("/users/" + userId)
                .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER")))
                .andExpect(status().isForbidden());

        // Usuário com papel ADMIN pode deletar
        mockMvc.perform(delete("/users/" + userId)
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/" + userId)
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("testDeleteUser_WithComplexRelationships: valida integridade após deleção com relacionamentos complexos")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDeleteUser_WithComplexRelationships() throws Exception {
        UserCreateRequest user = new UserCreateRequest("Complex Rel User", "complex.rel.user@example.com", "USER", null);
        UserResponse createdUser = userService.create(user);
        int userId = createdUser.id();

        // Criar posts e comentários relacionados
        userService.createPostForUser(userId, "Post A");
        userService.createPostForUser(userId, "Post B");
        userService.createCommentForUser(userId, "Comment 1");
        userService.createCommentForUser(userId, "Comment 2");

        mockMvc.perform(delete("/users/" + userId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/" + userId))
                .andExpect(status().isNotFound());

        // Verificar que posts e comentários foram removidos
        assertTrue(userService.getPostsByUserId(userId).isEmpty(), "Posts devem ser removidos após deleção");
        assertTrue(userService.getCommentsByUserId(userId).isEmpty(), "Comentários devem ser removidos após deleção");
    }
}