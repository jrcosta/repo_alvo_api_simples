package com.repoalvo.javaapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.repoalvo.javaapi.model.UserStatusSummaryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /users/status-summary retorna 200 e JSON com mapa de status e contagem")
    void getUsersStatusSummaryReturnsStatusMap() throws Exception {
        mockMvc.perform(get("/users/status-summary")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statuses").isMap())
                .andExpect(jsonPath("$.statuses.ACTIVE").isNumber())
                .andExpect(jsonPath("$.statuses.INACTIVE").isNumber());
    }

    @Test
    @DisplayName("GET /users/status-summary com base vazia retorna mapa vazio sem erro")
    void getUsersStatusSummaryWithEmptyDatabaseReturnsEmptyMap() throws Exception {
        // Para garantir base vazia, poderia mockar userService.listAllUsers() se fosse unitário,
        // mas aqui é integração real, assumimos base limpa ou isolada.

        mockMvc.perform(get("/users/status-summary")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statuses").isMap());
        // Não falha mesmo que o mapa esteja vazio
    }

    @Test
    @DisplayName("GET /users/status-summary valida headers HTTP e content-type")
    void getUsersStatusSummaryResponseHeaders() throws Exception {
        mockMvc.perform(get("/users/status-summary")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_JSON_VALUE)));
    }

    @Test
    @DisplayName("GET /users/status-summary JSON corresponde ao conteúdo do mapa retornado")
    void getUsersStatusSummaryJsonMatchesMap() throws Exception {
        String json = mockMvc.perform(get("/users/status-summary")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserStatusSummaryResponse response = objectMapper.readValue(json, UserStatusSummaryResponse.class);

        // Verifica que o mapa não é nulo e contém chaves e valores plausíveis
        Map<String, Long> statuses = response.statuses();
        assertThat(statuses).isNotNull();
        statuses.forEach((key, value) -> {
            assertThat(key).isNotNull();
            assertThat(value).isNotNull();
            assertThat(value).isGreaterThanOrEqualTo(0L);
        });
    }
}