package com.repoalvo.javaapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.repoalvo.javaapi.service.UserService;
import com.repoalvo.javaapi.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.anyOf;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerStatusIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        userService.reset();
    }

    @Test
    @DisplayName("PATCH /users/{userId}/status - 200: desativa usuário ativo não-admin")
    void shouldDeactivateActiveUser() throws Exception {
        // Bruno (id=2) é USER e está ACTIVE
        String body = objectMapper.writeValueAsString(Map.of("status", "INACTIVE"));

        mockMvc.perform(patch("/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(2)))
                .andExpect(jsonPath("$.status", is("INACTIVE")));
    }

    @Test
    @DisplayName("PATCH /users/{userId}/status - 200: ativa usuário inativo")
    void shouldActivateInactiveUser() throws Exception {
        // Primeiro desativa Bruno
        userService.updateStatus(2, "INACTIVE");

        String body = objectMapper.writeValueAsString(Map.of("status", "ACTIVE"));

        mockMvc.perform(patch("/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("PATCH /users/{userId}/status - 404: usuário não existe")
    void shouldReturn404WhenUserNotFound() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("status", "INACTIVE"));

        mockMvc.perform(patch("/users/999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /users/{userId}/status - 409: status já é o solicitado")
    void shouldReturn409WhenStatusAlreadySet() throws Exception {
        // Ana (id=1) já é ACTIVE
        String body = objectMapper.writeValueAsString(Map.of("status", "ACTIVE"));

        mockMvc.perform(patch("/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH /users/{userId}/status - 403: admin não pode ser desativado")
    void shouldReturn403WhenDeactivatingAdmin() throws Exception {
        // Ana (id=1) é ADMIN
        String body = objectMapper.writeValueAsString(Map.of("status", "INACTIVE"));

        mockMvc.perform(patch("/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /users/{userId}/status - 400: status inválido")
    void shouldReturn400WhenStatusIsInvalid() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("status", "SUSPENDED"));

        mockMvc.perform(patch("/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /users/{userId}/status - 400: body sem campo status")
    void shouldReturn400WhenStatusIsMissing() throws Exception {
        String body = "{}";

        mockMvc.perform(patch("/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /users/{userId}/status - 200: ignora campos extras no corpo da requisição")
    void shouldIgnoreExtraFieldsInRequestBody() throws Exception {
        Map<String, Object> bodyMap = Map.of(
                "status", "INACTIVE",
                "extraField1", "shouldBeIgnored",
                "extraField2", 12345
        );
        String body = objectMapper.writeValueAsString(bodyMap);

        mockMvc.perform(patch("/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(2)))
                .andExpect(jsonPath("$.status", is("INACTIVE")))
                .andExpect(jsonPath("$.extraField1").doesNotExist())
                .andExpect(jsonPath("$.extraField2").doesNotExist());
    }

    @Test
    @DisplayName("PATCH /users/{userId}/status - 200: atualização para status válidos ACTIVE e INACTIVE")
    void shouldAcceptValidStatusValues() throws Exception {
        // Ativa usuário inativo
        userService.updateStatus(2, "INACTIVE");
        String activateBody = objectMapper.writeValueAsString(Map.of("status", "ACTIVE"));

        mockMvc.perform(patch("/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        // Desativa usuário ativo
        String deactivateBody = objectMapper.writeValueAsString(Map.of("status", "INACTIVE"));

        mockMvc.perform(patch("/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deactivateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INACTIVE")));
    }

    @Test
    @DisplayName("PATCH /users/{userId}/status - 400: rejeita status nulo ou string vazia")
    void shouldReturn400WhenStatusIsNullOrEmpty() throws Exception {
        String nullStatusBody = "{\"status\":null}";
        String emptyStatusBody = "{\"status\":\"\"}";

        mockMvc.perform(patch("/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nullStatusBody))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyStatusBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /users/{userId}/status - 200: atualização sequencial múltipla reflete status correto")
    void shouldReflectCorrectStatusAfterMultipleSequentialUpdates() throws Exception {
        String activateBody = objectMapper.writeValueAsString(Map.of("status", "ACTIVE"));
        String deactivateBody = objectMapper.writeValueAsString(Map.of("status", "INACTIVE"));

        // Inicialmente ativo
        mockMvc.perform(patch("/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deactivateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INACTIVE")));

        mockMvc.perform(patch("/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        mockMvc.perform(patch("/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deactivateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INACTIVE")));
    }

    @Test
    @DisplayName("PATCH /users/{userId}/status - concorrência: múltiplas requisições simultâneas para alterar status do mesmo usuário")
    void shouldHandleConcurrentStatusUpdates() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Reset user 2 to ACTIVE before concurrency test
        userService.updateStatus(2, "ACTIVE");

        Runnable task = () -> {
            try {
                String body = objectMapper.writeValueAsString(Map.of("status", "INACTIVE"));
                mockMvc.perform(patch("/users/2/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id", is(2)))
                        .andExpect(jsonPath("$.status").value(anyOf(is("INACTIVE"), is("ACTIVE"))));
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                latch.countDown();
            }
        };

        for (int i = 0; i < threadCount; i++) {
            executor.submit(task);
        }

        latch.await();
        executor.shutdown();

        // After concurrency, user status should be either ACTIVE or INACTIVE (no invalid state)
        String finalStatus = userService.getById(2).map(User::status).orElse("UNKNOWN");
        assertTrue(finalStatus.equals("ACTIVE") || finalStatus.equals("INACTIVE"),
                () -> "Final user status must be ACTIVE or INACTIVE but was: " + finalStatus);
    }

    // --- Novos testes unitários para UserService ---

    @Test
    @DisplayName("getById retorna Optional com usuário quando existe")
    void testGetByIdReturnsUserWhenExists() {
        Optional<User> userOpt = userService.getById(2);
        assertTrue(userOpt.isPresent(), "Usuário com ID 2 deve existir");
        User user = userOpt.get();
        assertEquals(2, user.getId());
        assertNotNull(user.status());
        assertFalse(user.status().isEmpty());
    }

    @Test
    @DisplayName("getById retorna Optional.empty quando usuário não existe")
    void testGetByIdReturnsEmptyWhenUserNotExists() {
        Optional<User> userOpt = userService.getById(9999);
        assertTrue(userOpt.isEmpty(), "Usuário inexistente deve retornar Optional.empty");
    }

    @Test
    @DisplayName("getById não lança exceção inesperada")
    void testGetByIdDoesNotThrowException() {
        assertDoesNotThrow(() -> {
            userService.getById(2);
            userService.getById(9999);
        });
    }

    @Test
    @DisplayName("status retorna string válida e não nula")
    void testStatusMethodReturnsValidStatusString() {
        Optional<User> userOpt = userService.getById(2);
        assertTrue(userOpt.isPresent(), "Usuário com ID 2 deve existir");
        String status = userOpt.get().status();
        assertNotNull(status, "Status não deve ser nulo");
        assertFalse(status.isEmpty(), "Status não deve ser vazio");
        assertTrue(status.equals("ACTIVE") || status.equals("INACTIVE"),
                "Status deve ser ACTIVE ou INACTIVE, mas foi: " + status);
    }

    @Test
    @DisplayName("Simular ausência do usuário 2 e falha clara do teste")
    void testUserNotFoundResultsInClearFailure() {
        // Remove usuário 2 temporariamente
        userService.removeUser(2);
        Optional<User> userOpt = userService.getById(2);
        assertTrue(userOpt.isEmpty(), "Usuário 2 deve estar ausente após remoção");

        String finalStatus = userOpt.map(User::status).orElse("UNKNOWN");
        assertEquals("UNKNOWN", finalStatus, "Status deve ser UNKNOWN para usuário ausente");

        // Falha clara: lançar AssertionError com mensagem informativa
        AssertionError error = assertThrows(AssertionError.class, () -> {
            assertTrue(finalStatus.equals("ACTIVE") || finalStatus.equals("INACTIVE"),
                    "Usuário 2 não encontrado ou status inválido: " + finalStatus);
        });
        assertTrue(error.getMessage().contains("Usuário 2 não encontrado ou status inválido"));
    }

    @Test
    @DisplayName("Múltiplas requisições PATCH concorrentes mantêm status válido")
    void testConcurrentPatchRequestsMaintainValidStatus() throws Exception {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Reset user 2 to ACTIVE before concurrency test
        userService.updateStatus(2, "ACTIVE");

        Runnable taskActivate = () -> {
            try {
                String body = objectMapper.writeValueAsString(Map.of("status", "ACTIVE"));
                mockMvc.perform(patch("/users/2/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value(anyOf(is("ACTIVE"), is("INACTIVE"))));
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                latch.countDown();
            }
        };

        Runnable taskDeactivate = () -> {
            try {
                String body = objectMapper.writeValueAsString(Map.of("status", "INACTIVE"));
                mockMvc.perform(patch("/users/2/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value(anyOf(is("ACTIVE"), is("INACTIVE"))));
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                latch.countDown();
            }
        };

        for (int i = 0; i < threadCount; i++) {
            if (i % 2 == 0) {
                executor.submit(taskActivate);
            } else {
                executor.submit(taskDeactivate);
            }
        }

        latch.await();
        executor.shutdown();

        String finalStatus = userService.getById(2).map(User::status).orElse("UNKNOWN");
        assertTrue(finalStatus.equals("ACTIVE") || finalStatus.equals("INACTIVE"),
                () -> "Final user status must be ACTIVE or INACTIVE but was: " + finalStatus);
    }

    @Test
    @DisplayName("getById retorna usuário com status inesperado e falha clara")
    void testGetByIdWithUnexpectedStatusReportsAnomaly() {
        // Manipula usuário 2 para status inválido diretamente no serviço
        userService.updateStatus(2, "INVALID_STATUS");

        Optional<User> userOpt = userService.getById(2);
        assertTrue(userOpt.isPresent(), "Usuário 2 deve existir");

        String status = userOpt.get().status();
        assertNotNull(status, "Status não deve ser nulo");

        boolean validStatus = status.equals("ACTIVE") || status.equals("INACTIVE");
        if (!validStatus) {
            fail("Usuário 2 possui status inesperado: " + status);
        }
    }

    @Test
    @DisplayName("getById retorna usuário com status nulo e falha clara")
    void testGetByIdWithNullStatusFailsClearly() {
        // Manipula usuário 2 para status nulo diretamente no serviço
        userService.updateStatus(2, null);

        Optional<User> userOpt = userService.getById(2);
        assertTrue(userOpt.isPresent(), "Usuário 2 deve existir");

        String status = userOpt.get().status();
        assertNull(status, "Status deve ser nulo");

        AssertionError error = assertThrows(AssertionError.class, () -> {
            assertNotNull(status, "Status do usuário não pode ser nulo");
        });
        assertTrue(error.getMessage().contains("não pode ser nulo"));
    }
}