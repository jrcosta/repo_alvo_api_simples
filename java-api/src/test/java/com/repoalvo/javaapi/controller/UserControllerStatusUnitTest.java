package com.repoalvo.javaapi.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.repoalvo.javaapi.model.UserStatusUpdateRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import jakarta.validation.Validator;

@WebMvcTest(controllers = UserController.class)
public class UserControllerStatusUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Autowired
    private Validator validator;

    @BeforeEach
    void setup() {
        // Setup if needed
    }

    @Test
    void shouldAcceptValidStatusActiveAndReturn200() throws Exception {
        UserStatusUpdateRequest request = new UserStatusUpdateRequest("ACTIVE");
        when(userService.updateUserStatus(anyString(), eq("ACTIVE"))).thenReturn(true);

        mockMvc.perform(put("/users/{id}/status", "user123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(userService, times(1)).updateUserStatus("user123", "ACTIVE");
    }

    @Test
    void shouldAcceptValidStatusInactiveAndReturn200() throws Exception {
        UserStatusUpdateRequest request = new UserStatusUpdateRequest("INACTIVE");
        when(userService.updateUserStatus(anyString(), eq("INACTIVE"))).thenReturn(true);

        mockMvc.perform(put("/users/{id}/status", "user123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(userService, times(1)).updateUserStatus("user123", "INACTIVE");
    }

    @Test
    void shouldRejectRequestWithEmptyStatusAndReturn400() throws Exception {
        String json = "{\"status\":\"\"}";

        mockMvc.perform(put("/users/{id}/status", "user123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].message").value("O campo 'status' é obrigatório"));
    }

    @Test
    void shouldRejectRequestWithNullStatusAndReturn400() throws Exception {
        String json = "{\"status\":null}";

        mockMvc.perform(put("/users/{id}/status", "user123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].message").value("O campo 'status' é obrigatório"));
    }

    @Test
    void shouldRejectRequestWithInvalidStatusAndReturn400() throws Exception {
        String[] invalidStatuses = {"active", "PENDING", "INACTIVE ", " ACTIVE"};

        for (String invalid : invalidStatuses) {
            String json = String.format("{\"status\":\"%s\"}", invalid);

            mockMvc.perform(put("/users/{id}/status", "user123")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message").value("Status inválido. Valores aceitos: ACTIVE, INACTIVE"));
        }
    }

    @Test
    void shouldRejectRequestWithoutStatusFieldAndReturn400() throws Exception {
        String json = "{}";

        mockMvc.perform(put("/users/{id}/status", "user123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].message").value("O campo 'status' é obrigatório"));
    }

    @Test
    void shouldRejectRequestWithExtraFieldsAndReturn400() throws Exception {
        String json = "{\"status\":\"ACTIVE\", \"extra\":\"value\"}";

        mockMvc.perform(put("/users/{id}/status", "user123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest());
    }
}