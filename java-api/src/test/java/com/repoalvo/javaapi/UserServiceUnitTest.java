package com.repoalvo.javaapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.repoalvo.javaapi.model.UserCreateRequest;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;

class UserServiceUnitTest {

    private UserService userService;

    @BeforeEach
    void setup() {
        userService = new UserService();
    }

    @Test
    @DisplayName("createUser should persist phoneNumber correctly when valid phoneNumber is provided")
    void createUserShouldPersistValidPhoneNumber() {
        UserCreateRequest payload = new UserCreateRequest("Carlos", "carlos@example.com", "USER", "+55 11 91234-5678");

        UserResponse createdUser = userService.create(payload);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.phoneNumber()).isEqualTo("+55 11 91234-5678");
    }

    @Test
    @DisplayName("createUser should handle null phoneNumber by setting phoneNumber to null")
    void createUserShouldHandleNullPhoneNumber() {
        UserCreateRequest payload = new UserCreateRequest("Daniela", "daniela@example.com", "USER", null);

        UserResponse createdUser = userService.create(payload);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.phoneNumber()).isNull();
    }

    @Test
    @DisplayName("createUser should handle empty phoneNumber by setting phoneNumber to empty string")
    void createUserShouldHandleEmptyPhoneNumber() {
        UserCreateRequest payload = new UserCreateRequest("Eduardo", "eduardo@example.com", "USER", "");

        UserResponse createdUser = userService.create(payload);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.phoneNumber()).isEmpty();
    }

    @Nested
    @DisplayName("Parameterized tests for valid phoneNumber formats")
    class ValidPhoneNumberFormats {

        @ParameterizedTest(name = "Valid phoneNumber: {0}")
        @ValueSource(strings = {
                "+55 11 91234-5678",
                "+1 (555) 123-4567",
                "011 91234 5678",
                "912345678",
                "+44 20 7946 0958",
                "+55-11-91234-5678",
                "+55 (11) 91234 5678"
        })
        void createUserShouldAcceptValidPhoneNumbers(String phoneNumber) {
            UserCreateRequest payload = new UserCreateRequest("Test User", "testuser+" + phoneNumber.hashCode() + "@example.com", "USER", phoneNumber);

            UserResponse createdUser = userService.create(payload);

            assertThat(createdUser).isNotNull();
            assertThat(createdUser.phoneNumber()).isEqualTo(phoneNumber);
        }
    }

    @Nested
    @DisplayName("Parameterized tests for invalid phoneNumber formats")
    class InvalidPhoneNumberFormats {

        @ParameterizedTest(name = "Invalid phoneNumber: {0}")
        @ValueSource(strings = {
                "123",
                "abcde",
                "++55 11 91234-5678",
                "123-456-7890-1234-5678",
                "phone123!",
                "!!!@@@###",
                "     ",
                "\t\n"
        })
        void createUserShouldAcceptInvalidPhoneNumbersAsIs(String phoneNumber) {
            UserCreateRequest payload = new UserCreateRequest("Invalid User", "invaliduser+" + phoneNumber.hashCode() + "@example.com", "USER", phoneNumber);

            UserResponse createdUser = userService.create(payload);

            assertThat(createdUser).isNotNull();
            assertThat(createdUser.phoneNumber()).isEqualTo(phoneNumber);
        }
    }

    @Test
    @DisplayName("createUser should allow duplicate phoneNumber because no uniqueness rule exists")
    void createUserShouldAllowDuplicatePhoneNumber() {
        UserCreateRequest payload1 = new UserCreateRequest("User One", "userone@example.com", "USER", "+55 11 99999-9999");
        UserResponse created1 = userService.create(payload1);

        UserCreateRequest payload2 = new UserCreateRequest("User Two", "usertwo@example.com", "USER", "+55 11 99999-9999");
        UserResponse created2 = userService.create(payload2);

        assertThat(created1.phoneNumber()).isEqualTo(created2.phoneNumber());
        assertThat(userService.listAllUsers()).contains(created1, created2);
    }

    @Test
    @DisplayName("listAllUsers should return users with correct phoneNumber persisted")
    void listAllUsersShouldReturnUsersWithCorrectPhoneNumbers() {
        // Create users with different phone numbers
        UserCreateRequest payload1 = new UserCreateRequest("Alice", "alice@example.com", "USER", "+55 11 90000-0001");
        UserCreateRequest payload2 = new UserCreateRequest("Bob", "bob@example.com", "USER", "+55 11 90000-0002");

        UserResponse created1 = userService.create(payload1);
        UserResponse created2 = userService.create(payload2);

        List<UserResponse> users = userService.listAllUsers();

        assertThat(users).extracting(UserResponse::phoneNumber)
                .contains(created1.phoneNumber(), created2.phoneNumber());
    }

    @Test
    @DisplayName("UserCreateRequest serializes and deserializes correctly with various phoneNumber values")
    void userCreateRequestSerializationDeserialization() throws Exception {
        var objectMapper = new ObjectMapper();

        String[] phoneNumbers = {null, "", "+55 11 91234-5678", "invalid-phone-123!@#"};

        for (String phoneNumber : phoneNumbers) {
            UserCreateRequest original = new UserCreateRequest("Serialize Test", "serialize@example.com", "USER", phoneNumber);

            String json = objectMapper.writeValueAsString(original);
            UserCreateRequest deserialized = objectMapper.readValue(json, UserCreateRequest.class);

            assertThat(deserialized).isNotNull();
            assertThat(deserialized.name()).isEqualTo(original.name());
            assertThat(deserialized.email()).isEqualTo(original.email());
            assertThat(deserialized.role()).isEqualTo(original.role());
            assertThat(deserialized.phoneNumber()).isEqualTo(original.phoneNumber());
        }
    }

    @Test
    @DisplayName("UserCreateRequest deserializes numeric phoneNumber as string")
    void userCreateRequestDeserializesNumericPhoneNumberAsString() throws Exception {
        var objectMapper = new ObjectMapper();

        String json = """
                {
                    "name": "Numeric Phone",
                    "email": "numericphone@example.com",
                    "role": "USER",
                    "phoneNumber": 12345
                }
                """;

        UserCreateRequest deserialized = objectMapper.readValue(json, UserCreateRequest.class);

        assertThat(deserialized.phoneNumber()).isEqualTo("12345");
    }

    @Test
    @DisplayName("searchUsersByPhoneNumber should return all users with the same phoneNumber")
    void searchUsersByPhoneNumberShouldReturnAllMatchingUsers() {
        String duplicatePhone = "+55 11 88888-8888";

        UserCreateRequest payload1 = new UserCreateRequest("User A", "usera@example.com", "USER", duplicatePhone);
        UserCreateRequest payload2 = new UserCreateRequest("User B", "userb@example.com", "USER", duplicatePhone);
        UserCreateRequest payload3 = new UserCreateRequest("User C", "userc@example.com", "USER", "+55 11 77777-7777");

        UserResponse created1 = userService.create(payload1);
        UserResponse created2 = userService.create(payload2);
        userService.create(payload3);

        List<UserResponse> foundUsers = userService.searchByPhoneNumber(duplicatePhone);

        assertThat(foundUsers).hasSize(2);
        assertThat(foundUsers).extracting(UserResponse::phoneNumber).allMatch(pn -> pn.equals(duplicatePhone));
        assertThat(foundUsers).extracting(UserResponse::email).containsExactlyInAnyOrder("usera@example.com", "userb@example.com");
    }

    @Test
    @DisplayName("updateUser should update phoneNumber correctly even if duplicates exist")
    void updateUserShouldUpdatePhoneNumberWithDuplicates() {
        UserCreateRequest payload1 = new UserCreateRequest("User One", "userone@example.com", "USER", "+55 11 99999-9999");
        UserResponse created1 = userService.create(payload1);

        UserCreateRequest payload2 = new UserCreateRequest("User Two", "usertwo@example.com", "USER", "+55 11 99999-9999");
        UserResponse created2 = userService.create(payload2);

        UserCreateRequest updatePayload = new UserCreateRequest("User Two Updated", "usertwo@example.com", "USER", "+55 11 88888-8888");
        UserResponse updatedUser = userService.update(created2.id(), updatePayload);

        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.phoneNumber()).isEqualTo("+55 11 88888-8888");

        List<UserResponse> usersWithOldPhone = userService.searchByPhoneNumber("+55 11 99999-9999");
        assertThat(usersWithOldPhone).hasSize(1);
        assertThat(usersWithOldPhone.get(0).id()).isEqualTo(created1.id());

        List<UserResponse> usersWithNewPhone = userService.searchByPhoneNumber("+55 11 88888-8888");
        assertThat(usersWithNewPhone).hasSize(1);
        assertThat(usersWithNewPhone.get(0).id()).isEqualTo(created2.id());
    }

    @Test
    @DisplayName("deleteUser should remove only the specified user even if phoneNumber is duplicated")
    void deleteUserShouldRemoveOnlySpecifiedUserWithDuplicatePhoneNumber() {
        UserCreateRequest payload1 = new UserCreateRequest("User One", "userone@example.com", "USER", "+55 11 99999-9999");
        UserResponse created1 = userService.create(payload1);

        UserCreateRequest payload2 = new UserCreateRequest("User Two", "usertwo@example.com", "USER", "+55 11 99999-9999");
        UserResponse created2 = userService.create(payload2);

        userService.delete(created1.id());

        List<UserResponse> remainingUsers = userService.searchByPhoneNumber("+55 11 99999-9999");
        assertThat(remainingUsers).hasSize(1);
        assertThat(remainingUsers.get(0).id()).isEqualTo(created2.id());
    }

    @Test
    @DisplayName("createUser should accept phoneNumber with special characters and persist as is")
    void createUserShouldAcceptPhoneNumberWithSpecialCharacters() {
        String specialPhone = "+55 (11) 91234-5678 ext. 1234 #567";

        UserCreateRequest payload = new UserCreateRequest("Special Char User", "special@example.com", "USER", specialPhone);
        UserResponse createdUser = userService.create(payload);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.phoneNumber()).isEqualTo(specialPhone);
    }

    @Test
    @DisplayName("createUser should accept phoneNumber as numeric string and convert correctly")
    void createUserShouldConvertNumericPhoneNumberToString() {
        // Simulate numeric phone number input as string
        String numericPhone = "1234567890";

        UserCreateRequest payload = new UserCreateRequest("Numeric String User", "numericstring@example.com", "USER", numericPhone);
        UserResponse createdUser = userService.create(payload);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.phoneNumber()).isEqualTo(numericPhone);
    }

    @Test
    @DisplayName("createUser should accept phoneNumber as number and convert to string internally")
    void createUserShouldAcceptPhoneNumberAsNumberAndConvert() throws Exception {
        var objectMapper = new ObjectMapper();

        String json = """
                {
                    "name": "Number Phone",
                    "email": "numberphone@example.com",
                    "role": "USER",
                    "phoneNumber": 9876543210
                }
                """;

        UserCreateRequest deserialized = objectMapper.readValue(json, UserCreateRequest.class);
        UserResponse createdUser = userService.create(deserialized);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.phoneNumber()).isEqualTo("9876543210");
    }

    @Test
    @DisplayName("createUser should reject phoneNumber as JSON object or array")
    void createUserShouldRejectPhoneNumberAsObjectOrArray() {
        var objectMapper = new ObjectMapper();

        String jsonObject = """
                {
                    "name": "Object Phone",
                    "email": "objectphone@example.com",
                    "role": "USER",
                    "phoneNumber": {"number": "12345"}
                }
                """;

        String jsonArray = """
                {
                    "name": "Array Phone",
                    "email": "arrayphone@example.com",
                    "role": "USER",
                    "phoneNumber": ["12345"]
                }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(jsonObject, UserCreateRequest.class))
                .isInstanceOf(com.fasterxml.jackson.databind.exc.MismatchedInputException.class);

        assertThatThrownBy(() -> objectMapper.readValue(jsonArray, UserCreateRequest.class))
                .isInstanceOf(com.fasterxml.jackson.databind.exc.MismatchedInputException.class);
    }

    @Test
    @DisplayName("simulate concurrent creation of users with same phoneNumber to verify consistency")
    void simulateConcurrentCreationWithDuplicatePhoneNumber() throws InterruptedException {
        final int threadCount = 10;
        final String duplicatePhone = "+55 11 77777-7777";
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    UserCreateRequest payload = new UserCreateRequest(
                            "Concurrent User " + idx,
                            "concurrent" + idx + "@example.com",
                            "USER",
                            duplicatePhone
                    );
                    userService.create(payload);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        List<UserResponse> users = userService.searchByPhoneNumber(duplicatePhone);
        assertThat(users).hasSize(threadCount);
    }

    @Test
    @DisplayName("serializing and deserializing user preserves phoneNumber integrity with various formats")
    void serializationDeserializationPreservesPhoneNumberIntegrity() throws Exception {
        var objectMapper = new ObjectMapper();

        String[] phoneNumbers = {
                "+55 11 91234-5678",
                "1234567890",
                "",
                null,
                "invalid-phone-!@#",
                "+1 (555) 123-4567"
        };

        for (String phoneNumber : phoneNumbers) {
            UserCreateRequest original = new UserCreateRequest("Serialize Test", "serialize@example.com", "USER", phoneNumber);

            String json = objectMapper.writeValueAsString(original);
            UserCreateRequest deserialized = objectMapper.readValue(json, UserCreateRequest.class);

            assertThat(deserialized.phoneNumber()).isEqualTo(original.phoneNumber());
        }
    }
}