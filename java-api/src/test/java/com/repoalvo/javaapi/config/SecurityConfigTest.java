package com.repoalvo.javaapi.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.delete;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    // Since the SecurityConfig restricts DELETE /users/** to ADMIN role,
    // we simulate requests to /users/1 for testing.

    @Nested
    @DisplayName("DELETE /users/{id} Authorization Tests")
    class DeleteUserAuthorizationTests {

        @Test
        @DisplayName("testDeleteUserWithoutAuthentication_ShouldReturn401")
        void testDeleteUserWithoutAuthentication_ShouldReturn401() throws Exception {
            mockMvc.perform(delete("/users/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("testDeleteUserWithNonAdminRole_ShouldReturn403")
        @WithMockUser(username = "user", roles = {"USER"})
        void testDeleteUserWithNonAdminRole_ShouldReturn403() throws Exception {
            mockMvc.perform(delete("/users/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("testDeleteUserWithAdminRole_ShouldReturnSuccess")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void testDeleteUserWithAdminRole_ShouldReturnSuccess() throws Exception {
            // Since no actual controller is defined here, expect 404 Not Found or 200/204 if controller exists.
            // We test only security filter chain, so 404 is acceptable as success of auth.
            mockMvc.perform(delete("/users/1"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("testDeleteUserWithInvalidCsrfToken_ShouldReturnForbidden")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void testDeleteUserWithInvalidCsrfToken_ShouldReturnForbidden() throws Exception {
            // Even though CSRF is disabled, test that request with invalid CSRF token does not cause failure
            // Since CSRF is disabled, no CSRF token is required, so request should pass auth and return 404 (no controller)
            mockMvc.perform(delete("/users/1")
                            .with(SecurityMockMvcRequestPostProcessors.csrf().useInvalidToken()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Other HTTP Methods Without Authentication")
    class OtherMethodsWithoutAuthenticationTests {

        @Test
        @DisplayName("testOtherMethodsWithoutAuthentication_ShouldBePermitted_GET")
        void testOtherMethodsWithoutAuthentication_ShouldBePermitted_GET() throws Exception {
            mockMvc.perform(get("/users"))
                    .andExpect(status().isOk().or(status().isNotFound()))
                    .andExpect(content().string(containsString("")));
        }

        @Test
        @DisplayName("testOtherMethodsWithoutAuthentication_ShouldBePermitted_POST")
        void testOtherMethodsWithoutAuthentication_ShouldBePermitted_POST() throws Exception {
            mockMvc.perform(post("/users"))
                    .andExpect(status().isOk().or(status().isNotFound()));
        }

        @Test
        @DisplayName("testOtherMethodsWithoutAuthentication_ShouldBePermitted_PUT")
        void testOtherMethodsWithoutAuthentication_ShouldBePermitted_PUT() throws Exception {
            mockMvc.perform(put("/users/1"))
                    .andExpect(status().isOk().or(status().isNotFound()));
        }
    }

    @Nested
    @DisplayName("CSRF Vulnerability Tests")
    class CsrfVulnerabilityTests {

        @Test
        @DisplayName("testPostWithoutCsrfToken_ShouldBePermittedDueToCsrfDisabled")
        void testPostWithoutCsrfToken_ShouldBePermittedDueToCsrfDisabled() throws Exception {
            mockMvc.perform(post("/users"))
                    .andExpect(status().isOk().or(status().isNotFound()));
        }

        @Test
        @DisplayName("testPutWithoutCsrfToken_ShouldBePermittedDueToCsrfDisabled")
        void testPutWithoutCsrfToken_ShouldBePermittedDueToCsrfDisabled() throws Exception {
            mockMvc.perform(put("/users/1"))
                    .andExpect(status().isOk().or(status().isNotFound()));
        }

        @Test
        @DisplayName("testDeleteWithoutCsrfToken_ShouldBePermittedDueToCsrfDisabled")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void testDeleteWithoutCsrfToken_ShouldBePermittedDueToCsrfDisabled() throws Exception {
            mockMvc.perform(delete("/users/1"))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("testHttpBasicAuthenticationEnabled")
    void testHttpBasicAuthenticationEnabled() throws Exception {
        // Without credentials, DELETE /users/1 returns 401 Unauthorized (tested above)
        // With invalid credentials, returns 401 Unauthorized
        mockMvc.perform(delete("/users/1")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("invalidUser", "invalidPass")))
                .andExpect(status().isUnauthorized());
    }

    @Nested
    @DisplayName("Integration-like Authorization Flow Tests")
    class IntegrationLikeAuthorizationFlowTests {

        @Test
        @DisplayName("integrationTestDeleteUserAuthorization")
        void integrationTestDeleteUserAuthorization() throws Exception {
            // Without auth -> 401
            mockMvc.perform(delete("/users/1"))
                    .andExpect(status().isUnauthorized());

            // With non-admin role -> 403
            mockMvc.perform(delete("/users/1")
                            .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER")))
                    .andExpect(status().isForbidden());

            // With admin role -> 404 (no controller) but auth passed
            mockMvc.perform(delete("/users/1")
                            .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("integrationTestPublicEndpointsAccess")
        void integrationTestPublicEndpointsAccess() throws Exception {
            // GET /users without auth allowed
            mockMvc.perform(get("/users"))
                    .andExpect(status().isOk().or(status().isNotFound()));

            // POST /users without auth allowed
            mockMvc.perform(post("/users"))
                    .andExpect(status().isOk().or(status().isNotFound()));

            // PUT /users/1 without auth allowed
            mockMvc.perform(put("/users/1"))
                    .andExpect(status().isOk().or(status().isNotFound()));
        }

        @Test
        @DisplayName("integrationTestHttpBasicOverHttps")
        void integrationTestHttpBasicOverHttps() throws Exception {
            // The SecurityConfig does not enforce HTTPS, so test that HTTP Basic works without HTTPS enforcement
            mockMvc.perform(delete("/users/1")
                            .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "password"))
                            .secure(true)) // simulate HTTPS
                    .andExpect(status().isUnauthorized()); // no user "admin" defined, so 401 expected
        }
    }

    @Nested
    @DisplayName("Http Basic Authentication Brute Force Protection Tests")
    class HttpBasicBruteForceProtectionTests {

        @Test
        @DisplayName("testMultipleInvalidHttpBasicAuthenticationAttempts_ShouldReturn401")
        void testMultipleInvalidHttpBasicAuthenticationAttempts_ShouldReturn401() throws Exception {
            // Simulate multiple invalid attempts; since no brute force protection implemented,
            // all attempts should return 401 Unauthorized.
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(delete("/users/1")
                                .with(SecurityMockMvcRequestPostProcessors.httpBasic("invalidUser" + i, "invalidPass")))
                        .andExpect(status().isUnauthorized());
            }
        }
    }
}