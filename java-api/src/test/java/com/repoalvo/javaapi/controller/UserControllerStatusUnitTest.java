package com.repoalvo.javaapi.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.model.UserStatusUpdateRequest;
import com.repoalvo.javaapi.service.ExternalService;
import com.repoalvo.javaapi.service.UserService;

import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void setUp() {
        reset(userService);
    }

    @Test
    void testPatchUserStatusWithValidIntegerIdAndValidStatusActive() throws Exception {
        int userId = 10;
        UserResponse existing = new UserResponse(userId, "Alice", "alice@example.com", "INACTIVE", "USER");
        UserResponse updated = new UserResponse(userId, "Alice", "alice@example.com", "ACTIVE", "USER");

        when(userService.getById(userId)).thenReturn(Optional.of(existing));
        when(userService.updateStatus(userId, "ACTIVE")).thenReturn(Optional.of(updated));

        mockMvc.perform(patch("/users/{id}/status", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserStatusUpdateRequest("ACTIVE"))))
            .andExpect(status().isOk());

        verify(userService, times(1)).updateStatus(userId, "ACTIVE");
    }

    @Test
    void testPatchUserStatusWithValidIntegerIdAndValidStatusInactive() throws Exception {
        int userId = 11;
        UserResponse existing = new UserResponse(userId, "Bob", "bob@example.com", "ACTIVE", "USER");
        UserResponse updated = new UserResponse(userId, "Bob", "bob@example.com", "INACTIVE", "USER");

        when(userService.getById(userId)).thenReturn(Optional.of(existing));
        when(userService.updateStatus(userId, "INACTIVE")).thenReturn(Optional.of(updated));

        mockMvc.perform(patch("/users/{id}/status", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserStatusUpdateRequest("INACTIVE"))))
            .andExpect(status().isOk());

        verify(userService, times(1)).updateStatus(userId, "INACTIVE");
    }

    @Test
    void testPatchUserStatusWithInvalidIdFormatNegativeAndZero() throws Exception {
        int[] invalidIds = {-1, 0};

        for (int invalidId : invalidIds) {
            mockMvc.perform(patch("/users/{id}/status", invalidId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new UserStatusUpdateRequest("ACTIVE"))))
                .andExpect(status().isBadRequest());
        }
    }

    @Test
    void testPatchUserStatusRejectsNonPatchMethods() throws Exception {
        int userId = 5;
        String jsonPayload = objectMapper.writeValueAsString(new UserStatusUpdateRequest("ACTIVE"));

        mockMvc.perform(get("/users/{id}/status", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
            .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(post("/users/{id}/status", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
            .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(put("/users/{id}/status", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void testPatchUserStatusWithNonExistentUserReturns404() throws Exception {
        int userId = 9999;

        when(userService.getById(userId)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/users/{id}/status", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserStatusUpdateRequest("ACTIVE"))))
            .andExpect(status().isNotFound());

        verify(userService, never()).updateStatus(anyInt(), anyString());
    }

    @Test
    void testPatchUserStatusWithInvalidStatusValuesReturns400WithErrorMessage() throws Exception {
        int userId = 2;
        String[] invalidStatuses = {"", " ", "active", "PENDING", "INACTIVE ", " ACTIVE", "INVALID"};

        for (String invalidStatus : invalidStatuses) {
            mockMvc.perform(patch("/users/{id}/status", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.format("{\"status\":\"%s\"}", invalidStatus)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
        }
    }

    @Test
    void testPatchUserStatusRejectsExtraFieldsInPayload() throws Exception {
        int userId = 3;
        String payloadWithExtraFields = "{\"status\":\"ACTIVE\", \"extraField\":\"shouldNotBeHere\"}";

        UserResponse existing = new UserResponse(userId, "Carol", "carol@example.com", "INACTIVE", "USER");
        UserResponse updated = new UserResponse(userId, "Carol", "carol@example.com", "ACTIVE", "USER");

        when(userService.getById(userId)).thenReturn(Optional.of(existing));
        when(userService.updateStatus(userId, "ACTIVE")).thenReturn(Optional.of(updated));

        // Assuming controller ignores extra fields and processes normally
        mockMvc.perform(patch("/users/{id}/status", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadWithExtraFields))
            .andExpect(status().isOk());

        verify(userService, times(1)).updateStatus(userId, "ACTIVE");
    }

    @Test
    void testPatchUserStatusRejectsExtraNestedFieldsInPayload() throws Exception {
        int userId = 4;
        String payloadWithNestedExtra = "{\"status\":\"INACTIVE\", \"nested\":{\"field\":\"value\"}}";

        UserResponse existing = new UserResponse(userId, "Dave", "dave@example.com", "ACTIVE", "USER");
        UserResponse updated = new UserResponse(userId, "Dave", "dave@example.com", "INACTIVE", "USER");

        when(userService.getById(userId)).thenReturn(Optional.of(existing));
        when(userService.updateStatus(userId, "INACTIVE")).thenReturn(Optional.of(updated));

        // Assuming controller ignores nested extra fields and processes normally
        mockMvc.perform(patch("/users/{id}/status", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadWithNestedExtra))
            .andExpect(status().isOk());

        verify(userService, times(1)).updateStatus(userId, "INACTIVE");
    }

    @Test
    void testPatchUserStatusErrorResponseMessagesOnBadRequest() throws Exception {
        int userId = 7;

        // Missing status field
        mockMvc.perform(patch("/users/{id}/status", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.message").exists());

        // Null status field
        mockMvc.perform(patch("/users/{id}/status", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":null}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.message").exists());

        // Empty status field
        mockMvc.perform(patch("/users/{id}/status", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testPatchUserStatusWithStatusValuesWithSpacesAndCaseVariations() throws Exception {
        int userId = 8;
        UserResponse existing = new UserResponse(userId, "Eve", "eve@example.com", "INACTIVE", "USER");

        when(userService.getById(userId)).thenReturn(Optional.of(existing));

        // Variations that should be rejected (based on existing tests)
        String[] invalidStatuses = {" active", "ACTIVE ", "Active", "inActive", "INACTIVE ", " inactive"};

        for (String status : invalidStatuses) {
            mockMvc.perform(patch("/users/{id}/status", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.format("{\"status\":\"%s\"}", status)))
                .andExpect(status().isBadRequest());
        }
    }

    @Test
    void testPatchUserStatusWithIdIntegerLimits() throws Exception {
        int minInt = Integer.MIN_VALUE;
        int maxInt = Integer.MAX_VALUE;

        UserResponse existingMin = new UserResponse(minInt, "MinUser", "min@example.com", "INACTIVE", "USER");
        UserResponse updatedMin = new UserResponse(minInt, "MinUser", "min@example.com", "ACTIVE", "USER");

        UserResponse existingMax = new UserResponse(maxInt, "MaxUser", "max@example.com", "INACTIVE", "USER");
        UserResponse updatedMax = new UserResponse(maxInt, "MaxUser", "max@example.com", "ACTIVE", "USER");

        when(userService.getById(minInt)).thenReturn(Optional.of(existingMin));
        when(userService.updateStatus(minInt, "ACTIVE")).thenReturn(Optional.of(updatedMin));

        when(userService.getById(maxInt)).thenReturn(Optional.of(existingMax));
        when(userService.updateStatus(maxInt, "ACTIVE")).thenReturn(Optional.of(updatedMax));

        mockMvc.perform(patch("/users/{id}/status", minInt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserStatusUpdateRequest("ACTIVE"))))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/users/{id}/status", maxInt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserStatusUpdateRequest("ACTIVE"))))
            .andExpect(status().isOk());

        verify(userService, times(1)).updateStatus(minInt, "ACTIVE");
        verify(userService, times(1)).updateStatus(maxInt, "ACTIVE");
    }

    @Test
    void testIntegrationPatchUserStatusFlowSuccessAndFailure() throws Exception {
        int userId = 20;
        UserResponse existing = new UserResponse(userId, "Frank", "frank@example.com", "INACTIVE", "USER");
        UserResponse updated = new UserResponse(userId, "Frank", "frank@example.com", "ACTIVE", "USER");

        when(userService.getById(userId)).thenReturn(Optional.of(existing));
        when(userService.updateStatus(userId, "ACTIVE")).thenReturn(Optional.of(updated));

        // Success case
        mockMvc.perform(patch("/users/{id}/status", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserStatusUpdateRequest("ACTIVE"))))
            .andExpect(status().isOk());

        verify(userService, times(1)).updateStatus(userId, "ACTIVE");

        // Failure case: updateStatus returns empty Optional (simulate failure)
        when(userService.updateStatus(userId, "ACTIVE")).thenReturn(Optional.empty());

        mockMvc.perform(patch("/users/{id}/status", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserStatusUpdateRequest("ACTIVE"))))
            .andExpect(status().isInternalServerError());
    }
}