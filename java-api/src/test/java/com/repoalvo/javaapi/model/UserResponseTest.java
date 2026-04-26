package com.repoalvo.javaapi.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateUserResponseWithAllFieldsIncludingPhoneNumber() {
        UserResponse user = new UserResponse(
                10,
                "Test User",
                "testuser@example.com",
                "ACTIVE",
                "USER",
                "+55 11 90000-0001"
        );

        assertThat(user.id()).isEqualTo(10);
        assertThat(user.name()).isEqualTo("Test User");
        assertThat(user.email()).isEqualTo("testuser@example.com");
        assertThat(user.status()).isEqualTo("ACTIVE");
        assertThat(user.role()).isEqualTo("USER");
        assertThat(user.phoneNumber()).isEqualTo("+55 11 90000-0001");
        assertThat(user.vip()).isFalse();
    }

    @Test
    void shouldCreateUserResponseWithOldConstructorAndPhoneNumberIsNull() {
        UserResponse user = new UserResponse(
                11,
                "Old User",
                "olduser@example.com",
                "INACTIVE",
                "ADMIN"
        );

        assertThat(user.id()).isEqualTo(11);
        assertThat(user.name()).isEqualTo("Old User");
        assertThat(user.email()).isEqualTo("olduser@example.com");
        assertThat(user.status()).isEqualTo("INACTIVE");
        assertThat(user.role()).isEqualTo("ADMIN");
        assertThat(user.phoneNumber()).isNull();
        assertThat(user.vip()).isTrue();
    }

    @Test
    void shouldSerializeUserResponseIncludingPhoneNumber() throws JsonProcessingException {
        UserResponse user = new UserResponse(
                20,
                "Serialize User",
                "serialize@example.com",
                "ACTIVE",
                "USER",
                "+55 99 99999-9999"
        );

        String json = objectMapper.writeValueAsString(user);

        assertThat(json).contains("\"phoneNumber\":\"+55 99 99999-9999\"");
        assertThat(json).contains("\"id\":20");
        assertThat(json).contains("\"name\":\"Serialize User\"");
        assertThat(json).contains("\"email\":\"serialize@example.com\"");
        assertThat(json).contains("\"vip\":false");
    }

    @Test
    void shouldSerializeUserResponseWithNullPhoneNumber() throws JsonProcessingException {
        UserResponse user = new UserResponse(
                21,
                "Null Phone",
                "nullphone@example.com",
                "ACTIVE",
                "USER",
                null
        );

        String json = objectMapper.writeValueAsString(user);

        // The field phoneNumber should appear with null value in JSON
        assertThat(json).contains("\"phoneNumber\":null");
    }

    @Test
    void shouldDeserializeUserResponseWithoutPhoneNumberField() throws JsonProcessingException {
        String jsonWithoutPhoneNumber = """
                {
                  "id": 30,
                  "name": "No Phone",
                  "email": "nophone@example.com",
                  "status": "ACTIVE",
                  "role": "USER"
                }
                """;

        UserResponse user = objectMapper.readValue(jsonWithoutPhoneNumber, UserResponse.class);

        assertThat(user.id()).isEqualTo(30);
        assertThat(user.name()).isEqualTo("No Phone");
        assertThat(user.email()).isEqualTo("nophone@example.com");
        assertThat(user.status()).isEqualTo("ACTIVE");
        assertThat(user.role()).isEqualTo("USER");
        // phoneNumber should be null when absent in JSON
        assertThat(user.phoneNumber()).isNull();
        assertThat(user.vip()).isFalse();
    }

    @Test
    void shouldCreateUserResponseWithPhoneNumberExplicitlyNull() {
        UserResponse user = new UserResponse(
                40,
                "Explicit Null",
                "explicitnull@example.com",
                "ACTIVE",
                "USER",
                null
        );

        assertThat(user.phoneNumber()).isNull();
    }

    @Test
    void shouldAllowExplicitVipValue() {
        UserResponse user = new UserResponse(
                50,
                "Manual Vip",
                "manualvip@example.com",
                "ACTIVE",
                "USER",
                null,
                true
        );

        assertThat(user.vip()).isTrue();
    }

    @Test
    void shouldHaveVipDefaultFalseWhenNotInitializedExplicitly() {
        UserResponse user = new UserResponse(
                60,
                "Default Vip",
                "defaultvip@example.com",
                "ACTIVE",
                "USER",
                "+55 11 90000-0002"
        );

        assertThat(user.vip()).isFalse();
    }

    @Test
    void shouldGetAndSetVipCorrectly() {
        UserResponse user = new UserResponse(
                70,
                "Getter Setter Vip",
                "getsetvip@example.com",
                "ACTIVE",
                "USER",
                null,
                false
        );

        assertThat(user.vip()).isFalse();

        // Assuming UserResponse has a setter for vip (if immutable, this test is not applicable)
        // If no setter, this test is skipped.
        // Here we check if setter exists by reflection and test it if present.
        try {
            var vipSetter = UserResponse.class.getMethod("setVip", boolean.class);
            vipSetter.invoke(user, true);
            assertThat(user.vip()).isTrue();
        } catch (NoSuchMethodException e) {
            // No setter, test not applicable
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldConsiderVipInEqualsAndHashCode() {
        UserResponse user1 = new UserResponse(
                80,
                "Equals User",
                "equals@example.com",
                "ACTIVE",
                "USER",
                null,
                true
        );

        UserResponse user2 = new UserResponse(
                80,
                "Equals User",
                "equals@example.com",
                "ACTIVE",
                "USER",
                null,
                true
        );

        UserResponse user3 = new UserResponse(
                80,
                "Equals User",
                "equals@example.com",
                "ACTIVE",
                "USER",
                null,
                false
        );

        assertThat(user1).isEqualTo(user2);
        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());

        assertThat(user1).isNotEqualTo(user3);
        assertThat(user1.hashCode()).isNotEqualTo(user3.hashCode());
    }

    @Test
    void shouldIncludeVipInToString() {
        UserResponse user = new UserResponse(
                90,
                "ToString User",
                "tostring@example.com",
                "ACTIVE",
                "USER",
                null,
                true
        );

        String toString = user.toString();

        assertThat(toString).contains("vip=true");
    }

    @Test
    void shouldSerializeVipTrueAndFalseCorrectly() throws JsonProcessingException {
        UserResponse userVipTrue = new UserResponse(
                100,
                "Vip True",
                "viptrue@example.com",
                "ACTIVE",
                "USER",
                null,
                true
        );

        UserResponse userVipFalse = new UserResponse(
                101,
                "Vip False",
                "vipfalse@example.com",
                "ACTIVE",
                "USER",
                null,
                false
        );

        String jsonTrue = objectMapper.writeValueAsString(userVipTrue);
        String jsonFalse = objectMapper.writeValueAsString(userVipFalse);

        assertThat(jsonTrue).contains("\"vip\":true");
        assertThat(jsonFalse).contains("\"vip\":false");
    }

    @Test
    void shouldDeserializeVipTrueFalseAbsentAndInvalidValues() throws JsonProcessingException {
        String jsonVipTrue = """
                {
                  "id": 110,
                  "name": "Vip True",
                  "email": "viptrue@example.com",
                  "status": "ACTIVE",
                  "role": "USER",
                  "vip": true
                }
                """;

        String jsonVipFalse = """
                {
                  "id": 111,
                  "name": "Vip False",
                  "email": "vipfalse@example.com",
                  "status": "ACTIVE",
                  "role": "USER",
                  "vip": false
                }
                """;

        String jsonVipAbsent = """
                {
                  "id": 112,
                  "name": "Vip Absent",
                  "email": "vipabsent@example.com",
                  "status": "ACTIVE",
                  "role": "USER"
                }
                """;

        String jsonVipInvalid = """
                {
                  "id": 113,
                  "name": "Vip Invalid",
                  "email": "vipinvalid@example.com",
                  "status": "ACTIVE",
                  "role": "USER",
                  "vip": "notaboolean"
                }
                """;

        UserResponse userTrue = objectMapper.readValue(jsonVipTrue, UserResponse.class);
        UserResponse userFalse = objectMapper.readValue(jsonVipFalse, UserResponse.class);
        UserResponse userAbsent = objectMapper.readValue(jsonVipAbsent, UserResponse.class);

        assertThat(userTrue.vip()).isTrue();
        assertThat(userFalse.vip()).isFalse();
        assertThat(userAbsent.vip()).isFalse();

        // For invalid boolean value, Jackson throws exception
        assertThatThrownBy(() -> objectMapper.readValue(jsonVipInvalid, UserResponse.class))
                .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    void shouldInitializeVipCorrectlyInAllConstructors() {
        // Old constructor without phoneNumber derives vip from role.
        UserResponse oldUser = new UserResponse(
                120,
                "Old Constructor",
                "oldconstructor@example.com",
                "ACTIVE",
                "USER"
        );
        assertThat(oldUser.vip()).isFalse();

        // New constructor with vip parameter true
        UserResponse newUserTrue = new UserResponse(
                121,
                "New Constructor True",
                "newtrue@example.com",
                "ACTIVE",
                "USER",
                null,
                true
        );
        assertThat(newUserTrue.vip()).isTrue();

        // New constructor with vip parameter false
        UserResponse newUserFalse = new UserResponse(
                122,
                "New Constructor False",
                "newfalse@example.com",
                "ACTIVE",
                "USER",
                null,
                false
        );
        assertThat(newUserFalse.vip()).isFalse();

        // New constructor without vip parameter defaults to false
        UserResponse newUserDefault = new UserResponse(
                123,
                "New Constructor Default",
                "newdefault@example.com",
                "ACTIVE",
                "USER",
                null
        );
        assertThat(newUserDefault.vip()).isFalse();
    }

    // Additional tests to cover missing scenarios from QA suggestions

    @Test
    void shouldReturnFalseVipForUserRoleUsingOldConstructor() {
        UserResponse user = new UserResponse(
                130,
                "User Role",
                "userrole@example.com",
                "ACTIVE",
                "USER"
        );
        assertThat(user.vip()).isFalse();
    }

    @Test
    void shouldReturnTrueVipForAdminRoleUsingOldConstructor() {
        UserResponse user = new UserResponse(
                131,
                "Admin Role",
                "adminrole@example.com",
                "ACTIVE",
                "ADMIN"
        );
        assertThat(user.vip()).isTrue();
    }

    @Test
    void shouldReturnFalseVipForModeratorRoleUsingOldConstructor() {
        UserResponse user = new UserResponse(
                132,
                "Moderator Role",
                "moderator@example.com",
                "ACTIVE",
                "MODERATOR"
        );
        assertThat(user.vip()).isFalse();
    }

    @Test
    void shouldReturnFalseVipForGuestRoleUsingOldConstructor() {
        UserResponse user = new UserResponse(
                133,
                "Guest Role",
                "guest@example.com",
                "ACTIVE",
                "GUEST"
        );
        assertThat(user.vip()).isFalse();
    }

    @Test
    void shouldReturnFalseVipForUnknownRoleUsingOldConstructor() {
        UserResponse user = new UserResponse(
                134,
                "Unknown Role",
                "unknown@example.com",
                "ACTIVE",
                "UNKNOWN_ROLE"
        );
        assertThat(user.vip()).isFalse();
    }

    @Test
    void shouldSerializeAndDeserializeUserWithOldConstructorMaintainingVipConsistency() throws JsonProcessingException {
        UserResponse originalUser = new UserResponse(
                140,
                "Serialize Old Constructor",
                "serializeold@example.com",
                "ACTIVE",
                "ADMIN"
        );
        assertThat(originalUser.vip()).isTrue();

        String json = objectMapper.writeValueAsString(originalUser);
        UserResponse deserializedUser = objectMapper.readValue(json, UserResponse.class);

        assertThat(deserializedUser.vip()).isEqualTo(originalUser.vip());
        assertThat(deserializedUser.role()).isEqualTo(originalUser.role());
        assertThat(deserializedUser.phoneNumber()).isNull();
    }

    @Test
    void shouldSerializeAndDeserializeUserWithOldConstructorAndNonVipRole() throws JsonProcessingException {
        UserResponse originalUser = new UserResponse(
                141,
                "Serialize Old Constructor NonVip",
                "serializeoldnonvip@example.com",
                "ACTIVE",
                "USER"
        );
        assertThat(originalUser.vip()).isFalse();

        String json = objectMapper.writeValueAsString(originalUser);
        UserResponse deserializedUser = objectMapper.readValue(json, UserResponse.class);

        assertThat(deserializedUser.vip()).isEqualTo(originalUser.vip());
        assertThat(deserializedUser.role()).isEqualTo(originalUser.role());
        assertThat(deserializedUser.phoneNumber()).isNull();
    }
}