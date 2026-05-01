package com.repoalvo.javaapi.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.model.UserStatusUpdateRequest;
import com.repoalvo.javaapi.service.ExternalService;
import com.repoalvo.javaapi.service.UserService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerStatusUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private ExternalService externalService;

    @Test
    void shouldAcceptValidStatusActiveAndReturn200() throws Exception {
        int userId = 2;
        UserResponse existing = new UserResponse(userId, "Bruno Lima", "bruno@example.com", "INACTIVE", "USER");
        UserResponse updated = new UserResponse(userId, "Bruno Lima", "bruno@example.com", "ACTIVE", "USER");

        when(userService.getById(userId)).thenReturn(Optional.of(existing));
        when(userService.updateStatus(userId, "ACTIVE")).thenReturn(Optional.of(updated));

        mockMvc.perform(patch("/users/{id}/status", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserStatusUpdateRequest("ACTIVE"))))
            .andExpect(status().isOk());

        verify(userService, times(1)).updateStatus(userId, "ACTIVE");
    }

    @Test
    void shouldAcceptValidStatusInactiveAndReturn200() throws Exception {
        int userId = 2;
        UserResponse existing = new UserResponse(userId, "Bruno Lima", "bruno@example.com", "ACTIVE", "USER");
        UserResponse updated = new UserResponse(userId, "Bruno Lima", "bruno@example.com", "INACTIVE", "USER");

        when(userService.getById(userId)).thenReturn(Optional.of(existing));
        when(userService.updateStatus(userId, "INACTIVE")).thenReturn(Optional.of(updated));

        mockMvc.perform(patch("/users/{id}/status", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserStatusUpdateRequest("INACTIVE"))))
            .andExpect(status().isOk());

        verify(userService, times(1)).updateStatus(userId, "INACTIVE");
    }

    @Test
    void shouldRejectRequestWithEmptyStatusAndReturn400() throws Exception {
        mockMvc.perform(patch("/users/{id}/status", 2)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectRequestWithNullStatusAndReturn400() throws Exception {
        mockMvc.perform(patch("/users/{id}/status", 2)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":null}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectRequestWithInvalidStatusAndReturn400() throws Exception {
        String[] invalidStatuses = {"active", "PENDING", "INACTIVE ", " ACTIVE"};

        for (String invalid : invalidStatuses) {
            mockMvc.perform(patch("/users/{id}/status", 2)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.format("{\"status\":\"%s\"}", invalid)))
                .andExpect(status().isBadRequest());
        }
    }

    @Test
    void shouldRejectRequestWithoutStatusFieldAndReturn400() throws Exception {
        mockMvc.perform(patch("/users/{id}/status", 2)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }
}
