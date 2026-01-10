package io.nextskip.admin.internal.security;

import io.nextskip.test.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link SecurityConfig}.
 *
 * <p>Tests verify that route protection is correctly configured:
 * <ul>
 *   <li>/admin/** routes require authentication</li>
 *   <li>Public routes are accessible without authentication</li>
 * </ul>
 *
 * <p>Note: These tests verify the security configuration without actually
 * going through OAuth2. The OAuth2 flow itself is tested via E2E tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        // Enable OAuth2 config for security filter to activate
        "spring.security.oauth2.client.registration.github.client-id=test-client-id",
        "spring.security.oauth2.client.registration.github.client-secret=test-client-secret",
        "nextskip.admin.allowed-emails=admin@example.com",
        // Force Vaadin production mode (dev mode requires vaadin-dev dependency)
        "vaadin.productionMode=true"
})
@DisplayName("SecurityConfig Integration")
class SecurityConfigIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("unauthenticated user is redirected to OAuth2 login for admin routes")
    void testAdminRoute_Unauthenticated_RedirectsToLogin() {
        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        // Use exchange to capture redirect
        var response = restClient.get()
                .uri("/admin")
                .exchange((request, clientResponse) -> {
                    return new ResponseInfo(
                            HttpStatus.valueOf(clientResponse.getStatusCode().value()),
                            clientResponse.getHeaders().getLocation()
                    );
                });

        // Should redirect to GitHub OAuth2
        assertEquals(HttpStatus.FOUND, response.status(),
                "Should return 302 redirect for unauthenticated admin access");
        assertNotNull(response.location(), "Redirect location should be present");
        assertTrue(response.location().toString().contains("oauth2/authorization/github"),
                "Should redirect to GitHub OAuth2, but got: " + response.location());
    }

    @Test
    @DisplayName("public routes are accessible without authentication")
    void testPublicRoute_Unauthenticated_Allowed() {
        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        var response = restClient.get()
                .uri("/")
                .exchange((request, clientResponse) -> {
                    return new ResponseInfo(
                            HttpStatus.valueOf(clientResponse.getStatusCode().value()),
                            clientResponse.getHeaders().getLocation()
                    );
                });

        // Should NOT be 401/403 - public routes don't require auth
        assertNotEquals(HttpStatus.UNAUTHORIZED, response.status(),
                "Public route should not return 401");
        assertNotEquals(HttpStatus.FORBIDDEN, response.status(),
                "Public route should not return 403");
    }

    @Test
    @DisplayName("admin feeds route requires authentication")
    void testAdminFeedsRoute_Unauthenticated_RedirectsToLogin() {
        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        var response = restClient.get()
                .uri("/admin/feeds")
                .exchange((request, clientResponse) -> {
                    return new ResponseInfo(
                            HttpStatus.valueOf(clientResponse.getStatusCode().value()),
                            clientResponse.getHeaders().getLocation()
                    );
                });

        // Should redirect to GitHub OAuth2
        assertEquals(HttpStatus.FOUND, response.status(),
                "Should return 302 redirect for unauthenticated admin access");
        assertNotNull(response.location(), "Redirect location should be present");
        assertTrue(response.location().toString().contains("oauth2/authorization/github"),
                "Should redirect to GitHub OAuth2, but got: " + response.location());
    }

    private record ResponseInfo(HttpStatus status, URI location) { }
}
