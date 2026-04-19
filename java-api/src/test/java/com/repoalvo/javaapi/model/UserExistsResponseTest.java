package com.repoalvo.javaapi.model;

import com.fasterxml.jackson.core.JsonProcessingException;
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
    void shouldSerializeUserExistsResponseToJsonWithTrue() throws Exception {
        UserExistsResponse response = new UserExistsResponse(true);
        String json = objectMapper.writeValueAsString(response);
        assertEquals("{\"exists\":true}", json, "Serialized JSON should contain the exists field with true");
    }

    @Test
    void shouldSerializeUserExistsResponseToJsonWithFalse() throws Exception {
        UserExistsResponse response = new UserExistsResponse(false);
        String json = objectMapper.writeValueAsString(response);
        assertEquals("{\"exists\":false}", json, "Serialized JSON should contain the exists field with false");
    }

    @Test
    void shouldDeserializeJsonToUserExistsResponseWithFalse() throws Exception {
        String json = "{\"exists\":false}";
        UserExistsResponse response = objectMapper.readValue(json, UserExistsResponse.class);
        assertNotNull(response, "Deserialized object should not be null");
        assertFalse(response.exists(), "The exists field should be false after deserialization");
    }

    @Test
    void shouldDeserializeJsonToUserExistsResponseWithTrue() throws Exception {
        String json = "{\"exists\":true}";
        UserExistsResponse response = objectMapper.readValue(json, UserExistsResponse.class);
        assertNotNull(response, "Deserialized object should not be null");
        assertTrue(response.exists(), "The exists field should be true after deserialization");
    }

    @Test
    void shouldDeserializeJsonWithExtraFieldsIgnoringThem() throws Exception {
        String json = "{\"exists\":true, \"extraField\":\"extraValue\"}";
        UserExistsResponse response = objectMapper.readValue(json, UserExistsResponse.class);
        assertNotNull(response, "Deserialized object should not be null even with extra fields");
        assertTrue(response.exists(), "The exists field should be true after deserialization with extra fields");
    }

    @Test
    void shouldThrowExceptionWhenDeserializingInvalidJson() {
        String invalidJson = "{\"existss\":true}"; // typo in field name
        assertThrows(JsonProcessingException.class, () -> {
            objectMapper.readValue(invalidJson, UserExistsResponse.class);
        }, "Deserialization should fail with invalid JSON field");
    }

    @Test
    void shouldHaveConsistentEqualsAndHashCode() {
        UserExistsResponse responseTrue1 = new UserExistsResponse(true);
        UserExistsResponse responseTrue2 = new UserExistsResponse(true);
        UserExistsResponse responseFalse = new UserExistsResponse(false);

        // Reflexive
        assertEquals(responseTrue1, responseTrue1, "Equals should be reflexive");

        // Symmetric
        assertEquals(responseTrue1, responseTrue2, "Equals should be symmetric");
        assertEquals(responseTrue2, responseTrue1, "Equals should be symmetric");

        // Transitive
        UserExistsResponse responseTrue3 = new UserExistsResponse(true);
        assertEquals(responseTrue1, responseTrue2, "Equals should be transitive");
        assertEquals(responseTrue2, responseTrue3, "Equals should be transitive");
        assertEquals(responseTrue1, responseTrue3, "Equals should be transitive");

        // Consistent
        assertEquals(responseTrue1, responseTrue2, "Equals should be consistent");
        assertEquals(responseTrue1, responseTrue2, "Equals should be consistent");

        // Null comparison
        assertNotEquals(responseTrue1, null, "Equals should return false when compared to null");

        // Different values
        assertNotEquals(responseTrue1, responseFalse, "Objects with different exists values should not be equal");

        // HashCode consistency
        assertEquals(responseTrue1.hashCode(), responseTrue2.hashCode(), "Equal objects must have same hashCode");
        assertNotEquals(responseTrue1.hashCode(), responseFalse.hashCode(), "Different objects should have different hashCode");
    }
}