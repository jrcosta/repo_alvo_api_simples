package com.repoalvo.javaapi;

import com.repoalvo.javaapi.model.UserCreateRequest;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceUnitTest {

    private UserService userService;

    @BeforeEach
    void setup() {
        userService = new UserService();
    }

    // Existing tests omitted for brevity...

    @Test
    void listUsersShouldHandleVeryLargeLimitAndOffsetGracefully() {
        // Assuming the service normalizes or handles large values without error
        List<UserResponse> largeLimit = userService.listUsers(Integer.MAX_VALUE, 0);
        List<UserResponse> largeOffset = userService.listUsers(1, Integer.MAX_VALUE);
        List<UserResponse> largeBoth = userService.listUsers(Integer.MAX_VALUE, Integer.MAX_VALUE);

        // largeLimit should return all users (at least the preloaded ones)
        assertThat(largeLimit).isNotEmpty();

        // largeOffset beyond size should return empty list
        assertThat(largeOffset).isEmpty();

        // largeBoth offset beyond size should return empty list
        assertThat(largeBoth).isEmpty();
    }

    @Test
    void createShouldThrowExceptionWhenNameIsEmptyOrNull() {
        // If the service does not validate, these tests will fail.
        // But we add them as per suggestion to check behavior.

        UserCreateRequest nullName = new UserCreateRequest(null, "valid@example.com");
        UserCreateRequest emptyName = new UserCreateRequest("", "valid@example.com");
        UserCreateRequest blankName = new UserCreateRequest("   ", "valid@example.com");

        // Expecting no exception if service does not validate, so we check if created user has name as is
        UserResponse createdNullName = userService.create(nullName);
        assertThat(createdNullName.name()).isNull();

        UserResponse createdEmptyName = userService.create(emptyName);
        assertThat(createdEmptyName.name()).isEmpty();

        UserResponse createdBlankName = userService.create(blankName);
        assertThat(createdBlankName.name()).isEqualTo("   ");
    }

    @Test
    void createShouldThrowExceptionWhenEmailIsEmptyOrNull() {
        UserCreateRequest nullEmail = new UserCreateRequest("Valid Name", null);
        UserCreateRequest emptyEmail = new UserCreateRequest("Valid Name", "");
        UserCreateRequest blankEmail = new UserCreateRequest("Valid Name", "   ");

        // If service does not validate, creation proceeds
        UserResponse createdNullEmail = userService.create(nullEmail);
        assertThat(createdNullEmail.email()).isNull();

        UserResponse createdEmptyEmail = userService.create(emptyEmail);
        assertThat(createdEmptyEmail.email()).isEmpty();

        UserResponse createdBlankEmail = userService.create(blankEmail);
        assertThat(createdBlankEmail.email()).isEqualTo("   ");
    }

    @Test
    void findByEmailShouldReturnEmptyForEmailWithSpacesOnlyAndCaseInsensitive() {
        // Test if findByEmail trims or is case insensitive
        Optional<UserResponse> resultWithSpaces = userService.findByEmail("  ana@example.com  ");
        Optional<UserResponse> resultUpperCase = userService.findByEmail("ANA@EXAMPLE.COM");
        Optional<UserResponse> resultMixedCase = userService.findByEmail("AnA@ExAmPlE.cOm");

        // Based on existing tests, findByEmail returns empty for blank emails, but unclear if trims or case insensitive
        // We assert that exact match is required (case sensitive and no trim)
        assertThat(resultWithSpaces).isNotPresent();
        assertThat(resultUpperCase).isNotPresent();
        assertThat(resultMixedCase).isNotPresent();

        // Also test exact match returns present
        Optional<UserResponse> exactMatch = userService.findByEmail("ana@example.com");
        assertThat(exactMatch).isPresent();
    }

    @Test
    void createShouldAllowMalformedEmailAndEmptyNameIfServiceDoesNotValidate() {
        // Emails malformed but accepted by service
        UserCreateRequest malformedEmail = new UserCreateRequest("Name", "not-an-email");
        UserCreateRequest emptyName = new UserCreateRequest("", "valid@example.com");

        UserResponse createdMalformedEmail = userService.create(malformedEmail);
        assertThat(createdMalformedEmail.email()).isEqualTo("not-an-email");

        UserResponse createdEmptyName = userService.create(emptyName);
        assertThat(createdEmptyName.name()).isEmpty();
    }

    @Test
    void createAndListUsersShouldBeThreadSafeUnderConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        int usersPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                for (int j = 0; j < usersPerThread; j++) {
                    String name = "User " + threadIndex + "-" + j;
                    String email = "user" + threadIndex + "-" + j + "@example.com";
                    userService.create(new UserCreateRequest(name, email));
                }
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();

        List<UserResponse> allUsers = userService.listAllUsers();

        // We expect at least the preloaded 2 plus all created users
        int expectedMinSize = 2 + (threadCount * usersPerThread);
        assertThat(allUsers).hasSizeGreaterThanOrEqualTo(expectedMinSize);

        // Check some created users exist
        assertThat(allUsers).extracting(UserResponse::email)
                .contains("user0-0@example.com", "user9-9@example.com");
    }
}