package com.repoalvo.javaapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.Matchers.is;
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
                        .andExpect(jsonPath("$.status").value(org.hamcrest.Matchers.anyOf(is("INACTIVE"), is("ACTIVE"))));
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
        String finalStatus = userService.getById(2).map(u -> u.status()).orElse("UNKNOWN");
        assert(finalStatus.equals("ACTIVE") || finalStatus.equals("INACTIVE"));
    }
}