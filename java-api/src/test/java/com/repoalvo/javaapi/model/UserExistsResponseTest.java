package com.repoalvo.javaapi.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserExistsResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateUserExistsResponseWithTrue() {
        UserExistsResponse response = new UserExistsResponse(true);
        assertTrue(response.exists(), "The exists field should be true");
    }

    @Test
    void shouldCreateUserExistsResponseWithFalse() {
        UserExistsResponse response = new UserExistsResponse(false);
        assertFalse(response.exists(), "The exists field should be false");
    }

    @Test
    void shouldSerializeUserExistsResponseToJson() throws Exception {
        UserExistsResponse response = new UserExistsResponse(true);
        String json = objectMapper.writeValueAsString(response);
        assertEquals("{\"exists\":true}", json, "Serialized JSON should contain the exists field with true");
    }

    @Test
    void shouldDeserializeJsonToUserExistsResponse() throws Exception {
        String json = "{\"exists\":false}";
        UserExistsResponse response = objectMapper.readValue(json, UserExistsResponse.class);
        assertNotNull(response, "Deserialized object should not be null");
        assertFalse(response.exists(), "The exists field should be false after deserialization");
    }
}