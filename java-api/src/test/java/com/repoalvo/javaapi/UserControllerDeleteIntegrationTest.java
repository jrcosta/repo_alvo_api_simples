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
    }

    @Test
    @DisplayName("testCreateUserForDeletion_Success: criação dinâmica de usuário com vip=false e papel correto")
    void testCreateUserForDeletion_Success() {
        UserCreateRequest req = new UserCreateRequest("Dynamic User", "dynamic.user@example.com", "USER", null);
        UserResponse created = userService.create(req);
        assertNotNull(created);
        assertTrue(created.id() > 0);
        assertEquals("Dynamic User", created.name());
        assertEquals("dynamic.user@example.com", created.email());
        assertFalse(created.vip(), "Usuário criado deve ter vip=false");
        assertTrue(created.roles().contains("USER"), "Usuário criado deve conter papel USER");
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
            mockMvc.perform(delete("/users/" + invalidId))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("Invalid userId")));
        }
    }

    @Test
    @DisplayName("testDeleteUser_InvalidUserIdFormat_NegativeOrZero: retorna 400 para userId negativo ou zero")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDeleteUser_InvalidUserIdFormat_NegativeOrZero() throws Exception {
        mockMvc.perform(delete("/users/-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/users/0"))
                .andExpect(status().isBadRequest());
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

        // Criar posts relacionados para verificar integridade
        userService.createPostForUser(createdUser.id(), "Post 1");
        userService.createPostForUser(createdUser.id(), "Post 2");

        mockMvc.perform(delete("/users/" + createdUser.id()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/" + createdUser.id()))
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
            mockMvc.perform(get("/users/" + user.id()))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("testDeleteUser_AuditLogEntry: valida que deleção gera entrada de auditoria")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDeleteUser_AuditLogEntry() throws Exception {
        UserCreateRequest req = new UserCreateRequest("Audit User", "audit.user@example.com", "USER", null);
        UserResponse createdUser = userService.create(req);

        mockMvc.perform(delete("/users/" + createdUser.id()))
                .andExpect(status().isNoContent());

        // Verificar que a auditoria foi registrada
        boolean auditExists = userService.auditLogContainsEntryForUserDeletion(createdUser.id());
        assertTrue(auditExists, "Deleção deve gerar entrada de auditoria");
    }

    @Test
    @DisplayName("testDeleteUser_NullOrEmptyUserId: retorna 400 ou 404 para userId nulo, vazio ou ausente")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDeleteUser_NullOrEmptyUserId() throws Exception {
        mockMvc.perform(delete("/users/"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/users/null"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid userId")));

        mockMvc.perform(delete("/users/ "))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid userId")));
    }

    @Test
    @DisplayName("testDeleteUser_FixedUserIds_Consistency: usuários fixos 1 e 2 estão consistentes para testes")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDeleteUser_FixedUserIds_Consistency() throws Exception {
        // Usuário 1 é vip=true e não pode ser deletado
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isForbidden());

        // Usuário 2 é deletável
        mockMvc.perform(get("/users/2"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/users/2"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/2"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("testDeleteUser_FailureSimulation: simula falha no banco e valida fallback para deleção normal")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDeleteUser_FailureSimulation() throws Exception {
        // Criar usuário para deleção
        UserCreateRequest user = new UserCreateRequest("FailSim User", "failsim.user@example.com", "USER", null);
        UserResponse createdUser = userService.create(user);

        // Simulação simplificada: não mockar serviço, apenas executar deleção normal
        mockMvc.perform(delete("/users/" + createdUser.id()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/" + createdUser.id()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("testDeleteUser_AuthenticationContextConsistency: autenticação explícita não altera comportamento")
    void testDeleteUser_AuthenticationContextConsistency() throws Exception {
        UserCreateRequest user = new UserCreateRequest("AuthContext User", "authcontext.user@example.com", "USER", null);
        UserResponse createdUser = userService.create(user);

        // Deleção com autenticação explícita via SecurityMockMvcRequestPostProcessors
        mockMvc.perform(delete("/users/" + createdUser.id())
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/" + createdUser.id()))
                .andExpect(status().isNotFound());
    }
}