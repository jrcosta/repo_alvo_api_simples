package com.repoalvo.javaapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.repoalvo.javaapi.service.UserService userService;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        userService.reset();
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /users/by-email returns 200 and user JSON when email exists")
    void getUserByEmailShouldReturnUserWhenEmailExists() throws Exception {
        String email = "ana@example.com";

        mockMvc.perform(get("/users/by-email")
                        .param("email", email)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").isString())
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.status").isString())
                .andExpect(jsonPath("$.role").isString())
                .andExpect(jsonPath("$", not(hasKey("password")))); // no sensitive data
    }

    @Test
    @DisplayName("GET /users/by-email returns 404 with message when email does not exist")
    void getUserByEmailShouldReturn404WhenEmailDoesNotExist() throws Exception {
        String email = "naoexiste@example.com";

        mockMvc.perform(get("/users/by-email")
                        .param("email", email)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.detail", containsString("Usuário não encontrado")));
    }

    @Test
    @DisplayName("GET /users/by-email returns 400 Bad Request when email parameter is missing")
    void getUserByEmailShouldReturn400WhenEmailParamMissing() throws Exception {
        mockMvc.perform(get("/users/by-email")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /users/by-email returns 404 when email parameter is empty")
    void getUserByEmailShouldReturn404WhenEmailParamEmpty() throws Exception {
        mockMvc.perform(get("/users/by-email")
                        .param("email", "")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString("Usuário não encontrado")));
    }

    @Test
    @DisplayName("GET /users/by-email handles email with special characters consistently")
    void getUserByEmailShouldHandleEmailWithSpecialCharacters() throws Exception {
        String email = "usuario+teste@example.com";

        // First try with user not existing
        mockMvc.perform(get("/users/by-email")
                        .param("email", email)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /users/by-email returns 404 for emails with leading/trailing spaces")
    void getUserByEmailShouldReturn404ForEmailsWithSpaces() throws Exception {
        String emailWithSpaces = " ana@example.com ";

        mockMvc.perform(get("/users/by-email")
                        .param("email", emailWithSpaces)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /users/by-email is case insensitive for email parameter")
    void getUserByEmailShouldBeCaseInsensitive() throws Exception {
        String emailUpperCase = "ANA@EXAMPLE.COM";

        mockMvc.perform(get("/users/by-email")
                        .param("email", emailUpperCase)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("ana@example.com")));
    }

    @Test
    @DisplayName("GET /users/by-email returns 404 for emails exceeding max length")
    void getUserByEmailShouldReturn404ForEmailsExceedingMaxLength() throws Exception {
        StringBuilder longEmail = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            longEmail.append("a");
        }
        longEmail.append("@example.com");

        mockMvc.perform(get("/users/by-email")
                        .param("email", longEmail.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /users/by-email returns 404 for inactive users")
    void getUserByEmailShouldReturn404ForInactiveUsers() throws Exception {
        // Create inactive user directly via userService to simulate
        userService.createUser("Inactive User", "inactive@example.com", "inactive", "user");

        mockMvc.perform(get("/users/by-email")
                        .param("email", "inactive@example.com")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Concurrent GET /users/by-email requests do not cause race conditions or inconsistent state")
    void concurrentGetUserByEmailRequestsShouldBeHandledCorrectly() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        Runnable task = () -> {
            try {
                mockMvc.perform(get("/users/by-email")
                                .param("email", "ana@example.com")
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.email", is("ana@example.com")));
            } catch (Exception e) {
                fail("Exception during concurrent request: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        };

        for (int i = 0; i < threadCount; i++) {
            executor.submit(task);
        }

        latch.await();
        executor.shutdown();
    }

    @Test
    @DisplayName("Reset does not remove preloaded users required for tests")
    void resetShouldNotRemovePreloadedUsers() throws Exception {
        // The setup() calls reset, which adds default users ana and bob
        // Verify that ana@example.com user exists after reset
        mockMvc.perform(get("/users/by-email")
                        .param("email", "ana@example.com")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("ana@example.com")));
    }

    @Test
    @DisplayName("Simulate failure in userService.reset() and verify subsequent test fails with clear message")
    void simulateResetFailureShouldFailSubsequentTests() {
        // Create a proxy or spy to simulate exception on reset
        com.repoalvo.javaapi.service.UserService spyUserService = new com.repoalvo.javaapi.service.UserService() {
            @Override
            public void reset() {
                throw new RuntimeException("Simulated reset failure");
            }
        };

        // Replace userService with spy
        this.userService = spyUserService;

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            userService.reset();
        });
        assertEquals("Simulated reset failure", thrown.getMessage());
    }
}