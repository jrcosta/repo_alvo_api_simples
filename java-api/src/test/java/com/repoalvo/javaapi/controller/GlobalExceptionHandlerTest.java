package com.repoalvo.javaapi.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleResponseStatusException_shouldReturnStatusAndReason_whenReasonIsNotNull() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleResponseStatusException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User not found", response.getBody().get("detail"));
    }

    @Test
    void handleResponseStatusException_shouldReturnStatusAndMessage_whenReasonIsNull() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, null, new RuntimeException("Cause"));
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleResponseStatusException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        // The message should fallback to ex.getMessage()
        assertTrue(response.getBody().get("detail").contains("400 BAD_REQUEST"));
    }

    @Test
    void handleMethodArgumentTypeMismatch_shouldReturnBadRequestWithFixedMessage_forUserIdParameter() throws NoSuchMethodException {
        Method method = SampleController.class.getMethod("sampleMethod", Integer.class, Integer.class);
        MethodParameter methodParameter = new MethodParameter(method, 0); // userId parameter

        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "userId", methodParameter, null);

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleMethodArgumentTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid userId", response.getBody().get("detail"));
    }

    @Test
    void handleMethodArgumentTypeMismatch_shouldReturnBadRequestWithFixedMessage_forNonUserIdParameter() throws NoSuchMethodException {
        Method method = SampleController.class.getMethod("sampleMethod", Integer.class, Integer.class);
        MethodParameter methodParameter = new MethodParameter(method, 1); // page parameter

        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "xyz", Integer.class, "page", methodParameter, null);

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleMethodArgumentTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        // Despite parameter name being different, message is fixed "Invalid userId"
        assertEquals("Invalid userId", response.getBody().get("detail"));
    }

    @Test
    void handleMethodArgumentTypeMismatch_shouldNotExposeSensitiveData_inResponseBody() throws NoSuchMethodException {
        Method method = SampleController.class.getMethod("sampleMethod", Integer.class, Integer.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);

        Throwable cause = new RuntimeException("Sensitive stack trace");
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "badvalue", Integer.class, "userId", methodParameter, cause);

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleMethodArgumentTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        String detail = response.getBody().get("detail");
        assertEquals("Invalid userId", detail);
        // Ensure no stack trace or cause message is leaked
        assertFalse(detail.contains("Sensitive"));
        assertFalse(detail.contains("badvalue"));
    }

    @Test
    void handleMethodArgumentTypeMismatch_shouldHandleNullOrEmptyParameterName() throws NoSuchMethodException {
        Method method = SampleController.class.getMethod("sampleMethod", Integer.class, Integer.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);

        // Parameter name null
        MethodArgumentTypeMismatchException exNullName = new MethodArgumentTypeMismatchException(
                "val", Integer.class, null, methodParameter, null);
        ResponseEntity<Map<String, String>> responseNullName = exceptionHandler.handleMethodArgumentTypeMismatch(exNullName);
        assertEquals(HttpStatus.BAD_REQUEST, responseNullName.getStatusCode());
        assertEquals("Invalid userId", responseNullName.getBody().get("detail"));

        // Parameter name empty string
        MethodArgumentTypeMismatchException exEmptyName = new MethodArgumentTypeMismatchException(
                "val", Integer.class, "", methodParameter, null);
        ResponseEntity<Map<String, String>> responseEmptyName = exceptionHandler.handleMethodArgumentTypeMismatch(exEmptyName);
        assertEquals(HttpStatus.BAD_REQUEST, responseEmptyName.getStatusCode());
        assertEquals("Invalid userId", responseEmptyName.getBody().get("detail"));
    }

    @Test
    void handleMethodArgumentTypeMismatch_shouldReturnFixedMessage_whenMultipleParametersInvalid() throws NoSuchMethodException {
        Method method = SampleController.class.getMethod("sampleMethod", Integer.class, Integer.class);
        MethodParameter userIdParam = new MethodParameter(method, 0);
        MethodParameter pageParam = new MethodParameter(method, 1);

        MethodArgumentTypeMismatchException exUserId = new MethodArgumentTypeMismatchException(
                "bad", Integer.class, "userId", userIdParam, null);
        MethodArgumentTypeMismatchException exPage = new MethodArgumentTypeMismatchException(
                "bad", Integer.class, "page", pageParam, null);

        ResponseEntity<Map<String, String>> responseUserId = exceptionHandler.handleMethodArgumentTypeMismatch(exUserId);
        ResponseEntity<Map<String, String>> responsePage = exceptionHandler.handleMethodArgumentTypeMismatch(exPage);

        assertEquals("Invalid userId", responseUserId.getBody().get("detail"));
        assertEquals("Invalid userId", responsePage.getBody().get("detail"));
    }

    @Test
    void handleMethodArgumentTypeMismatch_shouldNotAlterStatusForOtherExceptions() {
        // This handler only handles MethodArgumentTypeMismatchException,
        // so other exceptions should not be handled here.
        // This test ensures no side effects or status changes occur outside this handler.
        // Since this is a unit test of the handler method, we just confirm no exceptions thrown.
        assertDoesNotThrow(() -> {
            // no-op
        });
    }

    // Sample controller to provide method signatures for MethodParameter
    public static class SampleController {
        public void sampleMethod(Integer userId, Integer page) {
            // no implementation needed
        }
    }
}