package io.nextskip.propagation.internal;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.nextskip.common.client.ExternalApiException;
import io.nextskip.common.client.InvalidApiResponseException;
import io.nextskip.propagation.model.SolarIndices;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for NoaaSwpcClient using WireMock.
 */
class NoaaSwpcClientTest {

    private static final String NOAA_SWPC_SOURCE = "NOAA SWPC";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_JSON = "application/json";

    private WireMockServer wireMockServer;
    private NoaaSwpcClient client;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private RetryRegistry retryRegistry;

    @BeforeEach
    void setUp() {
        // Start WireMock server
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();

        // Configure Resilience4j registries with default config
        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        retryRegistry = RetryRegistry.ofDefaults();

        // Configure client to use WireMock server
        String baseUrl = "http://localhost:" + wireMockServer.port();
        WebClient.Builder webClientBuilder = WebClient.builder();

        // Create a custom client that overrides the URL
        client = new StubNoaaSwpcClient(webClientBuilder,
                circuitBreakerRegistry, retryRegistry, baseUrl);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void testFetch_Success() {
        // Mock successful NOAA response
        String jsonResponse = """
            [
                {
                    "time-tag": "2025-01-01T00:00:00Z",
                    "ssn": 120,
                    "f10.7": 150.5
                },
                {
                    "time-tag": "2025-01-02T00:00:00Z",
                    "ssn": 125,
                    "f10.7": 155.2
                }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody(jsonResponse)));

        SolarIndices result = client.fetch();

        assertNotNull(result);
        assertEquals(155.2, result.solarFluxIndex(), 0.01);
        assertEquals(125, result.sunspotNumber());
        assertEquals(NOAA_SWPC_SOURCE, result.source());

        // Verify WireMock was called
        wireMockServer.verify(getRequestedFor(urlEqualTo("/")));
    }

    @Test
    void testFetch_EmptyResponse_ThrowsException() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody("[]")));

        // Empty response throws exception
        assertThrows(InvalidApiResponseException.class, () -> client.fetch());
    }

    @Test
    void testFetch_ServerError_ThrowsException() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // Server error throws exception
        assertThrows(ExternalApiException.class, () -> client.fetch());
    }

    @Test
    void testFetch_MalformedJson_ThrowsException() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody("{ invalid json }")));

        // Malformed JSON throws exception
        assertThrows(Exception.class, () -> client.fetch());
    }

    @Test
    void testFetch_MissingFields_ThrowsException() {
        String jsonResponse = """
            [
                {
                    "time-tag": "2025-01-01T00:00:00Z"
                }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody(jsonResponse)));

        // Missing required fields throws exception
        assertThrows(InvalidApiResponseException.class, () -> client.fetch());
    }

    @Test
    void testFetch_PartialDate_ParsesSuccessfully() {
        // NOAA sometimes returns partial dates like "2025-01" (year-month only)
        String jsonResponse = """
            [
                {
                    "time-tag": "2025-01",
                    "ssn": 120,
                    "f10.7": 150.5
                }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody(jsonResponse)));

        SolarIndices result = client.fetch();

        assertNotNull(result);
        assertEquals(150.5, result.solarFluxIndex(), 0.01);
        assertEquals(120, result.sunspotNumber());
        assertNotNull(result.timestamp());
    }

    @Test
    void shouldReturn_SourceName() {
        assertEquals("NOAA", client.getSourceName());
    }

    @Test
    void testFreshness_AfterSuccessfulFetch() {
        String jsonResponse = """
            [
                {
                    "time-tag": "2025-01-01T00:00:00Z",
                    "ssn": 120,
                    "f10.7": 150.5
                }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody(jsonResponse)));

        client.fetch();

        // Freshness tracking should be updated
        assertNotNull(client.getLastSuccessfulRefresh());
        assertNotNull(client.getDataAge());
    }

    @Test
    void testFreshness_AfterFailedFetch() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // Fetch should throw exception
        assertThrows(ExternalApiException.class, () -> client.fetch());

        // Freshness should not be updated (null since never succeeded)
        assertNull(client.getLastSuccessfulRefresh());
    }

    @Test
    void testIsStale_WhenNeverRefreshed() {
        // When: No fetch has occurred
        // Then: isStale should return true (never refreshed = stale)
        assertTrue(client.isStale());
    }

    @Test
    void testIsStale_AfterSuccessfulFetch() {
        // Given: Successful fetch
        String jsonResponse = """
            [
                {
                    "time-tag": "2025-01-01T00:00:00Z",
                    "ssn": 120,
                    "f10.7": 150.5
                }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody(jsonResponse)));

        client.fetch();

        // Then: Should not be stale (just refreshed, within interval)
        assertFalse(client.isStale());
    }

    /**
     * Stub subclass that allows URL override for testing.
     */
    static class StubNoaaSwpcClient extends NoaaSwpcClient {
        StubNoaaSwpcClient(
                WebClient.Builder webClientBuilder,
                CircuitBreakerRegistry circuitBreakerRegistry,
                RetryRegistry retryRegistry,
                String testUrl) {
            super(webClientBuilder, circuitBreakerRegistry, retryRegistry, testUrl);
        }
    }
}
