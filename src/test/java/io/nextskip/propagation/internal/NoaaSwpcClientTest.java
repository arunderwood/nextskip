package io.nextskip.propagation.internal;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.nextskip.propagation.model.SolarIndices;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.web.reactive.function.client.WebClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NoaaSwpcClient using WireMock.
 */
class NoaaSwpcClientTest {

    private WireMockServer wireMockServer;
    private NoaaSwpcClient client;
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // Start WireMock server
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();

        // Configure client to use WireMock server
        String baseUrl = "http://localhost:" + wireMockServer.port();
        WebClient.Builder webClientBuilder = WebClient.builder();
        cacheManager = new ConcurrentMapCacheManager("solarIndices");

        // Create a custom client that overrides the URL
        client = new TestNoaaSwpcClient(webClientBuilder, cacheManager, baseUrl);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void testFetchSolarIndices_Success() {
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
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        SolarIndices result = client.fetchSolarIndices();

        assertNotNull(result);
        assertEquals(155.2, result.solarFluxIndex(), 0.01);
        assertEquals(125, result.sunspotNumber());
        assertEquals("NOAA SWPC", result.source());

        // Verify WireMock was called
        wireMockServer.verify(getRequestedFor(urlEqualTo("/")));
    }

    @Test
    void testFetchSolarIndices_EmptyResponse() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        // Empty response should throw InvalidApiResponseException
        assertThrows(io.nextskip.propagation.internal.InvalidApiResponseException.class,
                () -> client.fetchSolarIndices());
    }

    @Test
    void testFetchSolarIndices_ServerError() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // Should throw exception which triggers fallback
        assertThrows(RuntimeException.class, () -> client.fetchSolarIndices());
    }

    @Test
    void testFetchSolarIndices_SlowResponse() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withFixedDelay(2000) // 2 second delay
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        // Slow response with empty data should throw InvalidApiResponseException
        assertThrows(io.nextskip.propagation.internal.InvalidApiResponseException.class,
                () -> client.fetchSolarIndices());
    }

    @Test
    void testFetchSolarIndices_MalformedJson() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ invalid json }")));

        assertThrows(RuntimeException.class, () -> client.fetchSolarIndices());
    }

    @Test
    void testParsing_MissingFields() {
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
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        // Missing required fields should throw InvalidApiResponseException during validation
        assertThrows(io.nextskip.propagation.internal.InvalidApiResponseException.class,
                () -> client.fetchSolarIndices());
    }

    /**
     * Test subclass that allows URL override for testing.
     */
    static class TestNoaaSwpcClient extends NoaaSwpcClient {
        public TestNoaaSwpcClient(WebClient.Builder webClientBuilder, CacheManager cacheManager, String testUrl) {
            super(webClientBuilder, cacheManager, testUrl);
        }
    }
}
