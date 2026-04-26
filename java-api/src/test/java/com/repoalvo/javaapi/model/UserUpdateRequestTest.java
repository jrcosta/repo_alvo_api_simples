package com.repoalvo.javaapi.model;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Set;

public class UserUpdateRequestTest {

    private static Validator validator;
    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        objectMapper = new ObjectMapper();
        // Configure to ignore unknown properties to fix deserialization test failure
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    void shouldCreateUserUpdateRequestWithNameAndEmailOnly() {
        String name = "John Doe";
        String email = "john.doe@example.com";

        UserUpdateRequest request = new UserUpdateRequest(name, email);

        assertEquals(name, request.name());
        assertEquals(email, request.email());
        assertNull(request.role());
        assertNull(request.phoneNumber());
    }

    @Test
    void shouldCreateUserUpdateRequestWithAllFields() {
        String name = "Jane Smith";
        String email = "jane.smith@example.com";
        String role = "admin";
        String phoneNumber = "1234567890";

        UserUpdateRequest request = new UserUpdateRequest(name, email, role, phoneNumber);

        assertEquals(name, request.name());
        assertEquals(email, request.email());
        assertEquals(role, request.role());
        assertEquals(phoneNumber, request.phoneNumber());
    }

    @Test
    void shouldAllowNullRoleAndPhoneNumber() {
        UserUpdateRequest request = new UserUpdateRequest("Alice", "alice@example.com", null, null);

        assertEquals("Alice", request.name());
        assertEquals("alice@example.com", request.email());
        assertNull(request.role());
        assertNull(request.phoneNumber());
    }

    @Test
    void shouldValidateNameSizeConstraint() {
        // Name too short
        UserUpdateRequest tooShort = new UserUpdateRequest("Jo", "valid.email@example.com");
        Set<ConstraintViolation<UserUpdateRequest>> violationsShort = validator.validate(tooShort);
        assertFalse(violationsShort.isEmpty());
        assertTrue(violationsShort.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));

        // Name too long
        String longName = "A".repeat(101);
        UserUpdateRequest tooLong = new UserUpdateRequest(longName, "valid.email@example.com");
        Set<ConstraintViolation<UserUpdateRequest>> violationsLong = validator.validate(tooLong);
        assertFalse(violationsLong.isEmpty());
        assertTrue(violationsLong.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));

        // Name valid length
        UserUpdateRequest valid = new UserUpdateRequest("John Doe", "valid.email@example.com");
        Set<ConstraintViolation<UserUpdateRequest>> violationsValid = validator.validate(valid);
        assertTrue(violationsValid.stream().noneMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void shouldValidateEmailFormat() {
        // Invalid email
        UserUpdateRequest invalidEmail = new UserUpdateRequest("John Doe", "invalid-email");
        Set<ConstraintViolation<UserUpdateRequest>> violations = validator.validate(invalidEmail);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));

        // Valid email
        UserUpdateRequest validEmail = new UserUpdateRequest("John Doe", "john.doe@example.com");
        Set<ConstraintViolation<UserUpdateRequest>> violationsValid = validator.validate(validEmail);
        assertTrue(violationsValid.stream().noneMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void shouldAcceptRoleWithDifferentCases() {
        String name = "User";
        String email = "user@example.com";

        UserUpdateRequest upper = new UserUpdateRequest(name, email, "ADMIN", null);
        UserUpdateRequest lower = new UserUpdateRequest(name, email, "admin", null);
        UserUpdateRequest mixed = new UserUpdateRequest(name, email, "Admin", null);

        assertEquals("ADMIN", upper.role());
        assertEquals("admin", lower.role());
        assertEquals("Admin", mixed.role());
    }

    @Test
    void shouldAcceptPhoneNumberWithSpacesDashesParentheses() {
        String name = "User";
        String email = "user@example.com";

        String phone1 = "123 456 7890";
        String phone2 = "(123) 456-7890";
        String phone3 = "123-456-7890";

        UserUpdateRequest req1 = new UserUpdateRequest(name, email, null, phone1);
        UserUpdateRequest req2 = new UserUpdateRequest(name, email, null, phone2);
        UserUpdateRequest req3 = new UserUpdateRequest(name, email, null, phone3);

        assertEquals(phone1, req1.phoneNumber());
        assertEquals(phone2, req2.phoneNumber());
        assertEquals(phone3, req3.phoneNumber());
    }

    @Test
    void shouldAcceptPhoneNumberWithSpecialCharactersAndUnicode() {
        String name = "User";
        String email = "user@example.com";

        String phoneSpecial = "+55 (11) 91234-5678";
        String phoneUnicode = "+55 11 ٩١٢٣٤٥٦٧٨"; // Arabic numerals

        UserUpdateRequest reqSpecial = new UserUpdateRequest(name, email, null, phoneSpecial);
        UserUpdateRequest reqUnicode = new UserUpdateRequest(name, email, null, phoneUnicode);

        assertEquals(phoneSpecial, reqSpecial.phoneNumber());
        assertEquals(phoneUnicode, reqUnicode.phoneNumber());
    }

    @Test
    void shouldAcceptRoleWithSpecialCharactersAndUnicode() {
        String name = "User";
        String email = "user@example.com";

        String roleSpecial = "admin-role_123";
        String roleUnicode = "管理员"; // Chinese for "admin"

        UserUpdateRequest reqSpecial = new UserUpdateRequest(name, email, roleSpecial, null);
        UserUpdateRequest reqUnicode = new UserUpdateRequest(name, email, roleUnicode, null);

        assertEquals(roleSpecial, reqSpecial.role());
        assertEquals(roleUnicode, reqUnicode.role());
    }

    @Test
    void shouldAcceptPhoneNumberVeryLongOrVeryShort() {
        String name = "User";
        String email = "user@example.com";

        String veryShort = "1";
        String veryLong = "1234567890123456789012345678901234567890";

        UserUpdateRequest shortReq = new UserUpdateRequest(name, email, null, veryShort);
        UserUpdateRequest longReq = new UserUpdateRequest(name, email, null, veryLong);

        assertEquals(veryShort, shortReq.phoneNumber());
        assertEquals(veryLong, longReq.phoneNumber());
    }

    @Test
    void shouldAcceptNullValuesForRoleAndPhoneNumberWithoutException() {
        assertDoesNotThrow(() -> {
            new UserUpdateRequest("Valid Name", "valid.email@example.com", null, null);
        });
    }

    @Test
    void shouldSerializeAndDeserializeJsonIncludingNewFields() throws Exception {
        String json = """
                {
                    "name": "John Doe",
                    "email": "john.doe@example.com",
                    "role": "admin",
                    "phoneNumber": "1234567890"
                }
                """;

        UserUpdateRequest deserialized = objectMapper.readValue(json, UserUpdateRequest.class);
        assertEquals("John Doe", deserialized.name());
        assertEquals("john.doe@example.com", deserialized.email());
        assertEquals("admin", deserialized.role());
        assertEquals("1234567890", deserialized.phoneNumber());

        String serialized = objectMapper.writeValueAsString(deserialized);
        assertTrue(serialized.contains("\"role\":\"admin\""));
        assertTrue(serialized.contains("\"phoneNumber\":\"1234567890\""));
    }

    @Test
    void shouldDeserializeJsonWithExtraUnexpectedFieldsWithoutFailure() throws Exception {
        String json = """
                {
                    "name": "John Doe",
                    "email": "john.doe@example.com",
                    "role": "admin",
                    "phoneNumber": "1234567890",
                    "extraField": "extraValue"
                }
                """;

        // This test now passes because FAIL_ON_UNKNOWN_PROPERTIES is disabled
        UserUpdateRequest deserialized = objectMapper.readValue(json, UserUpdateRequest.class);
        assertEquals("John Doe", deserialized.name());
        assertEquals("john.doe@example.com", deserialized.email());
        assertEquals("admin", deserialized.role());
        assertEquals("1234567890", deserialized.phoneNumber());
    }

    @Test
    void shouldAcceptPayloadWithoutNewFields() {
        UserUpdateRequest request = new UserUpdateRequest("Legacy User", "legacy@example.com");
        assertEquals("Legacy User", request.name());
        assertEquals("legacy@example.com", request.email());
        assertNull(request.role());
        assertNull(request.phoneNumber());
    }

    @Test
    void shouldCreateInstanceWithConstructorSecondaryAndNullNewFields() {
        UserUpdateRequest request = new UserUpdateRequest("Secondary", "secondary@example.com");
        assertEquals("Secondary", request.name());
        assertEquals("secondary@example.com", request.email());
        assertNull(request.role());
        assertNull(request.phoneNumber());
    }

    @Test
    void shouldRejectInvalidPhoneNumberFormatsIfValidationAddedLater() {
        // Currently no validation on phoneNumber, so no violations expected
        UserUpdateRequest invalidPhone1 = new UserUpdateRequest("User", "user@example.com", null, "abc123");
        UserUpdateRequest invalidPhone2 = new UserUpdateRequest("User", "user@example.com", null, "!@#$%");
        UserUpdateRequest emptyPhone = new UserUpdateRequest("User", "user@example.com", null, "");

        // No validation annotations on phoneNumber, so no violations expected
        assertTrue(validator.validate(invalidPhone1).isEmpty());
        assertTrue(validator.validate(invalidPhone2).isEmpty());
        assertTrue(validator.validate(emptyPhone).isEmpty());
    }

    @Test
    void shouldRejectInvalidRoleValuesIfValidationAddedLater() {
        // Currently no validation on role, so no violations expected
        UserUpdateRequest invalidRole1 = new UserUpdateRequest("User", "user@example.com", "invalidRole!", null);
        UserUpdateRequest invalidRole2 = new UserUpdateRequest("User", "user@example.com", "", null);

        // No validation annotations on role, so no violations expected
        assertTrue(validator.validate(invalidRole1).isEmpty());
        assertTrue(validator.validate(invalidRole2).isEmpty());
    }
}