package io.nextskip.propagation.internal;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.nextskip.common.client.InvalidApiResponseException;
import io.nextskip.propagation.model.SolarIndices;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.web.reactive.function.client.WebClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for NoaaSwpcClient using WireMock.
 */
class NoaaSwpcClientTest {

    private static final String NOAA_SWPC_SOURCE = "NOAA SWPC";
    private static final String DEGRADED_SOURCE_MARKER = "Degraded";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_JSON = "application/json";

    private WireMockServer wireMockServer;
    private NoaaSwpcClient client;
    private CacheManager cacheManager;
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
        cacheManager = new ConcurrentMapCacheManager("solarIndices");

        // Create a custom client that overrides the URL
        client = new StubNoaaSwpcClient(webClientBuilder, cacheManager,
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
    void testFetch_EmptyResponse_ReturnsDegradedData() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody("[]")));

        // Empty response triggers fallback to degraded data
        SolarIndices result = client.fetch();

        assertNotNull(result);
        assertTrue(result.source().contains(DEGRADED_SOURCE_MARKER));
    }

    @Test
    void testFetch_ServerError_ReturnsDegradedData() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // Server error triggers fallback to degraded data
        SolarIndices result = client.fetch();

        assertNotNull(result);
        assertTrue(result.source().contains(DEGRADED_SOURCE_MARKER));
    }

    @Test
    void testFetch_MalformedJson_ReturnsDegradedData() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody("{ invalid json }")));

        // Malformed JSON triggers fallback to degraded data
        SolarIndices result = client.fetch();

        assertNotNull(result);
        assertTrue(result.source().contains(DEGRADED_SOURCE_MARKER));
    }

    @Test
    void testFetch_MissingFields_ReturnsDegradedData() {
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

        // Missing required fields triggers fallback to degraded data
        SolarIndices result = client.fetch();

        assertNotNull(result);
        assertTrue(result.source().contains(DEGRADED_SOURCE_MARKER));
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
        assertFalse(client.isServingStaleData());
        assertNotNull(client.getDataAge());
    }

    @Test
    void testFreshness_AfterFailedFetch() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        client.fetch();

        // Should be serving stale/default data
        assertTrue(client.isServingStaleData());
    }

    /**
     * Stub subclass that allows URL override for testing.
     */
    static class StubNoaaSwpcClient extends NoaaSwpcClient {
        StubNoaaSwpcClient(
                WebClient.Builder webClientBuilder,
                CacheManager cacheManager,
                CircuitBreakerRegistry circuitBreakerRegistry,
                RetryRegistry retryRegistry,
                String testUrl) {
            super(webClientBuilder, cacheManager, circuitBreakerRegistry, retryRegistry, testUrl);
        }
    }
}
