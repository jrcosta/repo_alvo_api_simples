package com.repoalvo.javaapi.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class UserCreateRequestTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldCreateUserCreateRequestWithValidPhoneNumber() {
        UserCreateRequest request = new UserCreateRequest(
                "Helena Costa",
                "helena.costa@example.com",
                "USER",
                "+55 31 98888-7777"
        );

        assertThat(request.name()).isEqualTo("Helena Costa");
        assertThat(request.email()).isEqualTo("helena.costa@example.com");
        assertThat(request.role()).isEqualTo("USER");
        assertThat(request.phoneNumber()).isEqualTo("+55 31 98888-7777");
    }

    @Test
    void shouldCreateUserCreateRequestWithNullPhoneNumber() {
        UserCreateRequest request = new UserCreateRequest(
                "Igor Almeida",
                "igor.almeida@example.com",
                "ADMIN",
                null
        );

        assertThat(request.phoneNumber()).isNull();
    }

    @Test
    void shouldSerializeAndDeserializeUserCreateRequestWithPhoneNumber() throws Exception {
        UserCreateRequest original = new UserCreateRequest(
                "Joana Pereira",
                "joana.pereira@example.com",
                "USER",
                "+55 41 91234-5678"
        );

        String json = objectMapper.writeValueAsString(original);
        UserCreateRequest deserialized = objectMapper.readValue(json, UserCreateRequest.class);

        assertThat(deserialized).isNotNull();
        assertThat(deserialized.name()).isEqualTo(original.name());
        assertThat(deserialized.email()).isEqualTo(original.email());
        assertThat(deserialized.role()).isEqualTo(original.role());
        assertThat(deserialized.phoneNumber()).isEqualTo(original.phoneNumber());
    }

    @Test
    void shouldSerializeAndDeserializeUserCreateRequestWithNullPhoneNumber() throws Exception {
        UserCreateRequest original = new UserCreateRequest(
                "Lucas Fernandes",
                "lucas.fernandes@example.com",
                "USER",
                null
        );

        String json = objectMapper.writeValueAsString(original);
        UserCreateRequest deserialized = objectMapper.readValue(json, UserCreateRequest.class);

        assertThat(deserialized).isNotNull();
        assertThat(deserialized.phoneNumber()).isNull();
    }

    @Test
    void shouldCreateUserCreateRequestWithPhoneNumberContainingSpacesAndUnicode() {
        String phoneWithSpacesAndUnicode = "+55 11 9\u200B0000-0001"; // includes zero-width space
        UserCreateRequest request = new UserCreateRequest(
                "Mariana Silva",
                "mariana.silva@example.com",
                "USER",
                phoneWithSpacesAndUnicode
        );

        assertThat(request.phoneNumber()).isEqualTo(phoneWithSpacesAndUnicode);
    }

    @Test
    void shouldCreateUserCreateRequestWithPhoneNumberMaxLength() {
        // 20 characters phone number
        String maxLengthPhone = "+1234567890123456789";
        UserCreateRequest request = new UserCreateRequest(
                "Nicolas Souza",
                "nicolas.souza@example.com",
                "USER",
                maxLengthPhone
        );

        assertThat(request.phoneNumber()).hasSize(20);
        assertThat(request.phoneNumber()).isEqualTo(maxLengthPhone);
    }

    @Test
    void shouldCreateUserCreateRequestWithPhoneNumberInternationalPrefixes() {
        String[] internationalPhones = {
                "+1 202-555-0173",
                "+44 20 7946 0958",
                "+81 3 1234 5678",
                "+49 30 123456",
                "+86 10 1234 5678"
        };

        for (String phone : internationalPhones) {
            UserCreateRequest request = new UserCreateRequest(
                    "User " + phone,
                    "user" + phone.replaceAll("[^0-9]", "") + "@example.com",
                    "USER",
                    phone
            );
            assertThat(request.phoneNumber()).isEqualTo(phone);
        }
    }
}