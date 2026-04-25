package com.repoalvo.javaapi.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerUnitTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleResponseStatusException_withReasonPopulated_returnsStatusAndDetailFromReason() {
        String reason = "Custom reason message";
        HttpStatus status = HttpStatus.NOT_FOUND;
        ResponseStatusException ex = new ResponseStatusException(status, reason);

        ResponseEntity<Map<String, String>> response = handler.handleResponseStatusException(ex);

        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsOnlyKeys("detail");
        assertThat(response.getBody().get("detail")).isEqualTo(reason);
    }

    @Test
    void handleResponseStatusException_withNullReason_returnsStatusAndDetailFromMessage() {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        // reason null, message set
        ResponseStatusException ex = new ResponseStatusException(status, null);

        ResponseEntity<Map<String, String>> response = handler.handleResponseStatusException(ex);

        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsOnlyKeys("detail");
        // The detail should be the exception message, which includes status and reason (null here)
        assertThat(response.getBody().get("detail")).isEqualTo(ex.getMessage());
    }

    @Test
    void handleResponseStatusException_withEmptyReason_returnsStatusAndDetailFromMessage() {
        HttpStatus status = HttpStatus.CONFLICT;
        ResponseStatusException ex = new ResponseStatusException(status, "   ");

        ResponseEntity<Map<String, String>> response = handler.handleResponseStatusException(ex);

        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsOnlyKeys("detail");
        // Since reason is spaces, it is not null, so detail should be reason as is (spaces)
        assertThat(response.getBody().get("detail")).isEqualTo("   ");
    }

    @Test
    void handleResponseStatusException_withEmptyMessageAndNullReason_returnsStatusAndEmptyDetail() {
        // Create a subclass to override getMessage to return empty string
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT, null) {
            @Override
            public String getMessage() {
                return "";
            }
        };

        ResponseEntity<Map<String, String>> response = handler.handleResponseStatusException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.I_AM_A_TEAPOT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsOnlyKeys("detail");
        // Since reason is null and message is empty, detail should be empty string
        assertThat(response.getBody().get("detail")).isEmpty();
    }

    @Test
    void handleResponseStatusException_withNestedCause_returnsStatusAndDetailFromReason() {
        String reason = "Nested cause reason";
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        Throwable cause = new RuntimeException("Root cause");
        ResponseStatusException ex = new ResponseStatusException(status, reason, cause);

        ResponseEntity<Map<String, String>> response = handler.handleResponseStatusException(ex);

        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsOnlyKeys("detail");
        assertThat(response.getBody().get("detail")).isEqualTo(reason);
    }

    @Test
    void handleResponseStatusException_concurrentCalls_doNotInterfere() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    HttpStatus status = (idx % 2 == 0) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
                    String reason = "Reason " + idx;
                    ResponseStatusException ex = new ResponseStatusException(status, reason);
                    ResponseEntity<Map<String, String>> response = handler.handleResponseStatusException(ex);

                    assertThat(response.getStatusCode()).isEqualTo(status);
                    assertThat(response.getBody()).containsOnlyKeys("detail");
                    assertThat(response.getBody().get("detail")).isEqualTo(reason);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
    }
}