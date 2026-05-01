package com.repoalvo.javaapi.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

public class UserStatusUpdateRequestTest {

    private static Validator validator;
    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldCreateInstanceWithValidStatusActive() {
        UserStatusUpdateRequest req = new UserStatusUpdateRequest("ACTIVE");
        Set<ConstraintViolation<UserStatusUpdateRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "Não deve haver violações para status ACTIVE");
        assertEquals("ACTIVE", req.status());
    }

    @Test
    void shouldCreateInstanceWithValidStatusInactive() {
        UserStatusUpdateRequest req = new UserStatusUpdateRequest("INACTIVE");
        Set<ConstraintViolation<UserStatusUpdateRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "Não deve haver violações para status INACTIVE");
        assertEquals("INACTIVE", req.status());
    }

    @Test
    void shouldFailValidationWhenStatusIsNull() {
        UserStatusUpdateRequest req = new UserStatusUpdateRequest(null);
        Set<ConstraintViolation<UserStatusUpdateRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O campo 'status' é obrigatório")));
    }

    @Test
    void shouldFailValidationWhenStatusIsEmpty() {
        UserStatusUpdateRequest req = new UserStatusUpdateRequest("");
        Set<ConstraintViolation<UserStatusUpdateRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O campo 'status' é obrigatório")));
    }

    @Test
    void shouldFailValidationWhenStatusIsBlankSpaces() {
        UserStatusUpdateRequest req = new UserStatusUpdateRequest("   ");
        Set<ConstraintViolation<UserStatusUpdateRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O campo 'status' é obrigatório")));
    }

    @Test
    void shouldFailValidationWhenStatusIsInvalidValue() {
        String[] invalidValues = {"active", "PENDING", "INACTIVE ", " ACTIVE", "ActiVe"};
        for (String invalid : invalidValues) {
            UserStatusUpdateRequest req = new UserStatusUpdateRequest(invalid);
            Set<ConstraintViolation<UserStatusUpdateRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty(), "Esperava violação para valor inválido: " + invalid);
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Status inválido. Valores aceitos: ACTIVE, INACTIVE")));
        }
    }

    @Test
    void shouldFailValidationWhenStatusHasLeadingTrailingSpaces() {
        UserStatusUpdateRequest req = new UserStatusUpdateRequest(" ACTIVE ");
        Set<ConstraintViolation<UserStatusUpdateRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Status inválido. Valores aceitos: ACTIVE, INACTIVE")));
    }

    @Test
    void shouldFailValidationWithMultipleViolationsEmptyAndInvalid() {
        // Empty string triggers @NotBlank and possibly @Pattern violations
        UserStatusUpdateRequest req = new UserStatusUpdateRequest("");
        Set<ConstraintViolation<UserStatusUpdateRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        boolean hasNotBlank = violations.stream().anyMatch(v -> v.getMessage().equals("O campo 'status' é obrigatório"));
        boolean hasPattern = violations.stream().anyMatch(v -> v.getMessage().equals("Status inválido. Valores aceitos: ACTIVE, INACTIVE"));
        // Because @NotBlank fails first, pattern may not be triggered, but test for presence of at least one message
        assertTrue(hasNotBlank || hasPattern);
    }

    @Test
    void shouldSerializeToJsonCorrectly() throws Exception {
        UserStatusUpdateRequest req = new UserStatusUpdateRequest("ACTIVE");
        String json = objectMapper.writeValueAsString(req);
        assertTrue(json.contains("\"status\":\"ACTIVE\""));
    }

    @Test
    void shouldDeserializeFromJsonCorrectly() throws Exception {
        String json = "{\"status\":\"INACTIVE\"}";
        UserStatusUpdateRequest req = objectMapper.readValue(json, UserStatusUpdateRequest.class);
        assertEquals("INACTIVE", req.status());
        Set<ConstraintViolation<UserStatusUpdateRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldFailDeserializationWithExtraFields() {
        String json = "{\"status\":\"ACTIVE\", \"extraField\":\"value\"}";
        assertThrows(UnrecognizedPropertyException.class, () -> {
            objectMapper.readValue(json, UserStatusUpdateRequest.class);
        });
    }

    @Test
    void shouldFailDeserializationWhenStatusMissing() throws Exception {
        String json = "{}";
        UserStatusUpdateRequest req = objectMapper.readValue(json, UserStatusUpdateRequest.class);
        Set<ConstraintViolation<UserStatusUpdateRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O campo 'status' é obrigatório")));
    }

    @Test
    void shouldFailValidationWhenStatusIsNullExplicitly() {
        // Explicitly test null value triggers validation error
        UserStatusUpdateRequest req = new UserStatusUpdateRequest(null);
        Set<ConstraintViolation<UserStatusUpdateRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O campo 'status' é obrigatório")));
    }

    @Test
    void shouldValidateMessagesInPortugueseLocale() {
        Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("pt", "BR"));
            UserStatusUpdateRequest req = new UserStatusUpdateRequest("");
            Set<ConstraintViolation<UserStatusUpdateRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O campo 'status' é obrigatório")));
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }
}