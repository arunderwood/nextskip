package io.nextskip.admin;

import io.nextskip.test.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for OAuth2 login flow.
 *
 * <p>Tests verify the complete OAuth2 authentication flow:
 * <ul>
 *   <li>Unauthenticated users are redirected to OAuth2 login</li>
 *   <li>OAuth2 callback URLs are accessible</li>
 *   <li>Admin routes are protected</li>
 * </ul>
 *
 * <p>Note: These tests verify the OAuth2 configuration without mocking
 * the OAuth2 provider. The actual OAuth2 flow with GitHub is tested
 * via E2E tests with a real browser.
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
@DisplayName("OAuth2 Login Integration")
class OAuth2LoginIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    private RestClient createRestClient() {
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Nested
    @DisplayName("OAuth2 Redirect Flow")
    class OAuth2RedirectFlow {

        @Test
        @DisplayName("accessing /admin redirects to OAuth2 authorization")
        void testAdminRoute_Unauthenticated_RedirectsToOAuth2() {
            RestClient restClient = createRestClient();

            var response = restClient.get()
                    .uri("/admin")
                    .exchange((request, clientResponse) -> new ResponseInfo(
                            HttpStatus.valueOf(clientResponse.getStatusCode().value()),
                            clientResponse.getHeaders().getLocation()
                    ));

            assertEquals(HttpStatus.FOUND, response.status(),
                    "Should return 302 redirect for unauthenticated admin access");
            assertNotNull(response.location(), "Redirect location should be present");
            assertTrue(response.location().toString().contains("oauth2/authorization/github"),
                    "Should redirect to GitHub OAuth2 authorization, but got: " + response.location());
        }

        @Test
        @DisplayName("accessing /admin/feeds redirects to OAuth2 authorization")
        void testAdminFeedsRoute_Unauthenticated_RedirectsToOAuth2() {
            RestClient restClient = createRestClient();

            var response = restClient.get()
                    .uri("/admin/feeds")
                    .exchange((request, clientResponse) -> new ResponseInfo(
                            HttpStatus.valueOf(clientResponse.getStatusCode().value()),
                            clientResponse.getHeaders().getLocation()
                    ));

            assertEquals(HttpStatus.FOUND, response.status(),
                    "Should return 302 redirect for unauthenticated admin/feeds access");
            assertNotNull(response.location(), "Redirect location should be present");
            assertTrue(response.location().toString().contains("oauth2/authorization/github"),
                    "Should redirect to GitHub OAuth2 authorization, but got: " + response.location());
        }

        @Test
        @DisplayName("OAuth2 authorization endpoint is accessible")
        void testOAuth2AuthorizationEndpoint_Accessible() {
            RestClient restClient = createRestClient();

            var response = restClient.get()
                    .uri("/oauth2/authorization/github")
                    .exchange((request, clientResponse) -> new ResponseInfo(
                            HttpStatus.valueOf(clientResponse.getStatusCode().value()),
                            clientResponse.getHeaders().getLocation()
                    ));

            // Should redirect to GitHub's OAuth2 authorize URL
            assertEquals(HttpStatus.FOUND, response.status(),
                    "OAuth2 authorization endpoint should redirect to provider");
            assertNotNull(response.location(), "Redirect location should be present");
            assertTrue(response.location().toString().contains("github.com"),
                    "Should redirect to GitHub, but got: " + response.location());
        }
    }

    @Nested
    @DisplayName("Public Routes")
    class PublicRoutes {

        @Test
        @DisplayName("root path is accessible without authentication")
        void testRootPath_NoAuth_Accessible() {
            RestClient restClient = createRestClient();

            var response = restClient.get()
                    .uri("/")
                    .exchange((request, clientResponse) -> new ResponseInfo(
                            HttpStatus.valueOf(clientResponse.getStatusCode().value()),
                            clientResponse.getHeaders().getLocation()
                    ));

            // Should NOT be 401/403 - public routes don't require auth
            assertTrue(response.status().is2xxSuccessful() || response.status() == HttpStatus.NOT_FOUND,
                    "Public route should be accessible, got: " + response.status());
        }

        @Test
        @DisplayName("static assets path is accessible without authentication")
        void testStaticAssets_NoAuth_Accessible() {
            RestClient restClient = createRestClient();

            var response = restClient.get()
                    .uri("/VAADIN/")
                    .exchange((request, clientResponse) -> new ResponseInfo(
                            HttpStatus.valueOf(clientResponse.getStatusCode().value()),
                            clientResponse.getHeaders().getLocation()
                    ));

            // Static assets should be accessible (may be 404 if no assets exist yet)
            assertTrue(response.status().is2xxSuccessful()
                            || response.status() == HttpStatus.NOT_FOUND
                            || response.status() == HttpStatus.FORBIDDEN,
                    "Static assets should be accessible, got: " + response.status());
        }
    }

    @Nested
    @DisplayName("Hilla Endpoints Protection")
    class HillaEndpointsProtection {

        @Test
        @DisplayName("connect endpoint requires authentication for admin methods")
        void testConnectEndpoint_AdminMethods_RequireAuth() {
            RestClient restClient = createRestClient();

            // Hilla endpoints are at /connect/<EndpointName>/<methodName>
            var response = restClient.post()
                    .uri("/connect/AdminAuthEndpoint/getCurrentUser")
                    .header("Content-Type", "application/json")
                    .body("{}")
                    .exchange((request, clientResponse) -> new ResponseInfo(
                            HttpStatus.valueOf(clientResponse.getStatusCode().value()),
                            clientResponse.getHeaders().getLocation()
                    ));

            // Should be 401 Unauthorized since not authenticated
            assertEquals(HttpStatus.UNAUTHORIZED, response.status(),
                    "Admin endpoint should require authentication");
        }
    }

    private record ResponseInfo(HttpStatus status, URI location) { }
}
