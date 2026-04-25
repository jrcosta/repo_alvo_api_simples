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
        assertNull(response.userId(), "The userId field should be null when using single-arg constructor");
    }

    @Test
    void shouldCreateUserExistsResponseWithFalse() {
        UserExistsResponse response = new UserExistsResponse(false);
        assertFalse(response.exists(), "The exists field should be false");
        assertNull(response.userId(), "The userId field should be null when using single-arg constructor");
    }

    @Test
    void shouldCreateUserExistsResponseWithExistsAndUserId() {
        UserExistsResponse response = new UserExistsResponse(true, 123);
        assertTrue(response.exists(), "The exists field should be true");
        assertEquals(123, response.userId(), "The userId field should be 123");
    }

    @Test
    void shouldSerializeUserExistsResponseToJsonWithOnlyExistsWhenUserIdIsNull() throws Exception {
        UserExistsResponse response = new UserExistsResponse(true, null);
        String json = objectMapper.writeValueAsString(response);
        assertEquals("{\"exists\":true}", json, "Serialized JSON should contain only the exists field when userId is null");
    }

    @Test
    void shouldSerializeUserExistsResponseToJsonWithExistsAndUserId() throws Exception {
        UserExistsResponse response = new UserExistsResponse(true, 123);
        String json = objectMapper.writeValueAsString(response);
        assertTrue(json.contains("\"exists\":true"), "Serialized JSON should contain the exists field");
        assertTrue(json.contains("\"userId\":123"), "Serialized JSON should contain the userId field");
    }

    @Test
    void shouldDeserializeJsonToUserExistsResponseWithOnlyExists() throws Exception {
        String json = "{\"exists\":true}";
        UserExistsResponse response = objectMapper.readValue(json, UserExistsResponse.class);
        assertNotNull(response, "Deserialized object should not be null");
        assertTrue(response.exists(), "The exists field should be true after deserialization");
        assertNull(response.userId(), "The userId field should be null when not present in JSON");
    }

    @Test
    void shouldDeserializeJsonToUserExistsResponseWithExistsAndUserId() throws Exception {
        String json = "{\"exists\":true,\"userId\":123}";
        UserExistsResponse response = objectMapper.readValue(json, UserExistsResponse.class);
        assertNotNull(response, "Deserialized object should not be null");
        assertTrue(response.exists(), "The exists field should be true after deserialization");
        assertEquals(123, response.userId(), "The userId field should be 123 after deserialization");
    }

    @Test
    void shouldCreateUserExistsResponseUsingSecondaryConstructor() {
        UserExistsResponse response = new UserExistsResponse(true);
        assertTrue(response.exists(), "The exists field should be true");
        assertNull(response.userId(), "The userId field should be null when using secondary constructor");
    }

    @Test
    void shouldSerializeUserExistsResponseWithUserIdZero() throws Exception {
        UserExistsResponse response = new UserExistsResponse(true, 0);
        String json = objectMapper.writeValueAsString(response);
        assertTrue(json.contains("\"userId\":0"), "Serialized JSON should contain userId with value 0");
    }

    @Test
    void shouldSerializeUserExistsResponseWithUserIdNegative() throws Exception {
        UserExistsResponse response = new UserExistsResponse(true, -1);
        String json = objectMapper.writeValueAsString(response);
        assertTrue(json.contains("\"userId\":-1"), "Serialized JSON should contain userId with negative value");
    }

    @Test
    void shouldSerializeUserExistsResponseWithUserIdMaxValue() throws Exception {
        UserExistsResponse response = new UserExistsResponse(true, Integer.MAX_VALUE);
        String json = objectMapper.writeValueAsString(response);
        assertTrue(json.contains("\"userId\":" + Integer.MAX_VALUE), "Serialized JSON should contain userId with max int value");
    }

    @Test
    void shouldDeserializeUserExistsResponseWithUserIdZero() throws Exception {
        String json = "{\"exists\":true,\"userId\":0}";
        UserExistsResponse response = objectMapper.readValue(json, UserExistsResponse.class);
        assertEquals(0, response.userId(), "Deserialized userId should be 0");
        assertTrue(response.exists(), "Deserialized exists should be true");
    }

    @Test
    void shouldDeserializeUserExistsResponseWithUserIdNegative() throws Exception {
        String json = "{\"exists\":true,\"userId\":-10}";
        UserExistsResponse response = objectMapper.readValue(json, UserExistsResponse.class);
        assertEquals(-10, response.userId(), "Deserialized userId should be -10");
        assertTrue(response.exists(), "Deserialized exists should be true");
    }

    @Test
    void shouldDeserializeUserExistsResponseWithUserIdMaxValue() throws Exception {
        String json = "{\"exists\":true,\"userId\":" + Integer.MAX_VALUE + "}";
        UserExistsResponse response = objectMapper.readValue(json, UserExistsResponse.class);
        assertEquals(Integer.MAX_VALUE, response.userId(), "Deserialized userId should be max int value");
        assertTrue(response.exists(), "Deserialized exists should be true");
    }

    @Test
    void shouldAllowExistsFalseWithNonNullUserId() {
        UserExistsResponse response = new UserExistsResponse(false, 999);
        assertFalse(response.exists(), "The exists field should be false");
        assertEquals(999, response.userId(), "The userId field should be 999");
    }

    @Test
    void equalsAndHashCodeShouldConsiderUserId() {
        UserExistsResponse response1 = new UserExistsResponse(true, 123);
        UserExistsResponse response2 = new UserExistsResponse(true, 123);
        UserExistsResponse response3 = new UserExistsResponse(true, 456);
        UserExistsResponse response4 = new UserExistsResponse(false, 123);

        assertEquals(response1, response2, "Objects with same exists and userId should be equal");
        assertEquals(response1.hashCode(), response2.hashCode(), "Hash codes should be equal for equal objects");

        assertNotEquals(response1, response3, "Objects with different userId should not be equal");
        assertNotEquals(response1, response4, "Objects with different exists should not be equal");
    }

    @Test
    void shouldFailDeserializationWithInvalidUserIdType() {
        String json = "{\"exists\":true,\"userId\":\"invalidString\"}";
        assertThrows(JsonProcessingException.class, () -> objectMapper.readValue(json, UserExistsResponse.class),
                "Deserialization should fail when userId is not an integer");
    }

    @Test
    void shouldFailDeserializationWithInvalidUserIdBoolean() {
        String json = "{\"exists\":true,\"userId\":true}";
        assertThrows(JsonProcessingException.class, () -> objectMapper.readValue(json, UserExistsResponse.class),
                "Deserialization should fail when userId is a boolean");
    }
}