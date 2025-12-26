package io.nextskip.activations.internal;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SotaClient using WireMock to simulate the SOTA API.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Test JSON fixtures intentionally duplicated
class SotaClientTest {

    private static final String SOTA_API_SOURCE = "SOTA API";
    private static final String W1ABC_PORTABLE = "W1ABC/P";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_JSON = "application/json";

    private WireMockServer wireMockServer;
    private SotaClient sotaClient;
    private CacheManager cacheManager;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private RetryRegistry retryRegistry;

    @BeforeEach
    void setUp() {
        // Start WireMock server
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();

        // Configure Resilience4j registries with default config
        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        retryRegistry = RetryRegistry.ofDefaults();

        // Configure client to use WireMock server
        String baseUrl = wireMockServer.baseUrl();
        WebClient.Builder webClientBuilder = WebClient.builder();
        cacheManager = new ConcurrentMapCacheManager("sotaActivations");

        sotaClient = new StubSotaClient(webClientBuilder, cacheManager,
                circuitBreakerRegistry, retryRegistry, baseUrl);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void shouldFetch_RecentSotaActivationsSuccessfully() {
        // Given: Mock SOTA API response with recent and old spots
        Instant now = Instant.now();
        Instant recentTime = now.minus(5, ChronoUnit.MINUTES);
        Instant oldTime = now.minus(50, ChronoUnit.MINUTES);

        String jsonResponse = String.format("""
            [
              {
                "id": 123456,
                "activatorCallsign": "W1ABC/P",
                "associationCode": "W7W",
                "summitCode": "W7W/LC-001",
                "summitDetails": "Mount Test",
                "frequency": "14.250",
                "mode": "SSB",
                "timeStamp": "%s"
              },
              {
                "id": 123457,
                "activatorCallsign": "K2DEF/P",
                "associationCode": "W1",
                "summitCode": "W1/HA-001",
                "summitDetails": "Old Summit",
                "frequency": "7.200",
                "mode": "CW",
                "timeStamp": "%s"
              }
            ]
            """, recentTime.truncatedTo(ChronoUnit.SECONDS).toString().replace("Z", ""),
                oldTime.truncatedTo(ChronoUnit.SECONDS).toString().replace("Z", ""));

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = sotaClient.fetch();

        // Then: Should return only recent activation (within 45 minutes)
        assertNotNull(result);
        assertEquals(1, result.size(), "Should filter to only recent spots");

        Activation activation = result.get(0);
        assertEquals("123456", activation.spotId());
        assertEquals(W1ABC_PORTABLE, activation.activatorCallsign());
        assertEquals(ActivationType.SOTA, activation.type());
        assertEquals(14250.0, activation.frequency(), "Frequency should be converted from MHz to kHz");
        assertEquals("SSB", activation.mode());
        assertNull(activation.qsoCount(), "SOTA doesn't provide QSO count");
        assertEquals(SOTA_API_SOURCE, activation.source());
        assertNotNull(activation.spottedAt());

        // Verify location data (Summit object)
        assertNotNull(activation.location());
        assertEquals("W7W/LC-001", activation.location().reference());
        assertEquals("Mount Test", activation.location().name());
        assertEquals("WA", activation.location().regionCode(), "Should map W7W association to WA state");
        assertEquals("W7W", ((io.nextskip.activations.model.Summit) activation.location()).associationCode());
    }

    @Test
    void shouldFilter_OutOldSpots() {
        // Given: All spots are older than 45 minutes
        Instant oldTime = Instant.now().minus(50, ChronoUnit.MINUTES);

        String jsonResponse = String.format("""
            [
              {
                "id": 123456,
                "activatorCallsign": "W1ABC/P",
                "summitCode": "W7W/LC-001",
                "summitDetails": "Mount Test",
                "frequency": "14.250",
                "mode": "SSB",
                "timeStamp": "%s"
              }
            ]
            """, oldTime.truncatedTo(ChronoUnit.SECONDS).toString().replace("Z", ""));

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = sotaClient.fetch();

        // Then: Should return empty list
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Should filter out spots older than 45 minutes");
    }

    @Test
    void shouldHandle_EmptyResponseGracefully() {
        // Given: Empty array response
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody("[]")));

        // When: Fetch activations
        List<Activation> result = sotaClient.fetch();

        // Then: Should return empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandle_NullFieldsGracefully() {
        // Given: Response with null/missing fields
        Instant recentTime = Instant.now().minus(5, ChronoUnit.MINUTES);

        String jsonResponse = String.format("""
            [
              {
                "id": 123456,
                "activatorCallsign": "W1ABC/P",
                "summitCode": "W7W/LC-001",
                "summitDetails": "Mount Test",
                "frequency": null,
                "mode": null,
                "timeStamp": "%s"
              }
            ]
            """, recentTime.truncatedTo(ChronoUnit.SECONDS).toString().replace("Z", ""));

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = sotaClient.fetch();

        // Then: Should parse successfully with null fields
        assertNotNull(result);
        assertEquals(1, result.size());

        Activation activation = result.get(0);
        assertEquals(W1ABC_PORTABLE, activation.activatorCallsign());
        assertNull(activation.frequency());
        assertNull(activation.mode());
    }

    @Test
    void shouldConvert_FrequencyFromMHzToKHz() {
        // Given: Frequency in MHz format
        Instant recentTime = Instant.now().minus(5, ChronoUnit.MINUTES);

        String jsonResponse = String.format("""
            [
              {
                "id": 123456,
                "activatorCallsign": "W1ABC/P",
                "summitCode": "W7W/LC-001",
                "summitDetails": "Mount Test",
                "frequency": "3.573",
                "mode": "FT8",
                "timeStamp": "%s"
              }
            ]
            """, recentTime.truncatedTo(ChronoUnit.SECONDS).toString().replace("Z", ""));

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = sotaClient.fetch();

        // Then: Should convert MHz to kHz
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(3573.0, result.get(0).frequency(), 0.01);
    }

    @Test
    void shouldHandle_HttpErrorWithFallback() {
        // Given: API returns 500 error
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // When: Fetch activations
        List<Activation> result = sotaClient.fetch();

        // Then: Should return empty list (fallback)
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandle_NetworkErrorWithFallback() {
        // Given: Simulate network error by stopping server
        wireMockServer.stop();

        // When: Fetch activations
        List<Activation> result = sotaClient.fetch();

        // Then: Should return empty list (fallback)
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldParse_TimestampWithoutZSuffix() {
        // Given: SOTA format without 'Z' suffix
        Instant recentTime = Instant.now().minus(5, ChronoUnit.MINUTES);

        String jsonResponse = String.format("""
            [
              {
                "id": 123456,
                "activatorCallsign": "W1ABC/P",
                "summitCode": "W7W/LC-001",
                "summitDetails": "Mount Test",
                "frequency": "14.250",
                "mode": "SSB",
                "timeStamp": "%s"
              }
            ]
            """, recentTime.truncatedTo(ChronoUnit.SECONDS).toString().replace("Z", ""));

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = sotaClient.fetch();

        // Then: Should parse timestamp correctly
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).spottedAt());
    }

    @Test
    void shouldParse_TimestampWithZSuffix() {
        // Given: ISO-8601 format with 'Z' suffix
        Instant recentTime = Instant.now().minus(5, ChronoUnit.MINUTES);

        String jsonResponse = String.format("""
            [
              {
                "id": 123456,
                "activatorCallsign": "W1ABC/P",
                "summitCode": "W7W/LC-001",
                "summitDetails": "Mount Test",
                "frequency": "14.250",
                "mode": "SSB",
                "timeStamp": "%s"
              }
            ]
            """, recentTime.truncatedTo(ChronoUnit.SECONDS).toString());

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = sotaClient.fetch();

        // Then: Should parse timestamp correctly
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).spottedAt());
    }

    @Test
    void shouldHandle_InvalidTimestampGracefully() {
        // Given: Invalid timestamp format - will use current time
        String jsonResponse = """
            [
              {
                "id": 123456,
                "activatorCallsign": "W1ABC/P",
                "summitCode": "W7W/LC-001",
                "summitDetails": "Mount Test",
                "frequency": "14.250",
                "mode": "SSB",
                "timeStamp": "invalid-timestamp"
              }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = sotaClient.fetch();

        // Then: Should use current time as fallback (which will pass recency filter)
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).spottedAt());

        // Timestamp should be recent (within last minute)
        Instant now = Instant.now();
        Instant spottedAt = result.get(0).spottedAt();
        assertTrue(spottedAt.isAfter(now.minusSeconds(60)));
    }

    @Test
    void shouldReturn_SourceName() {
        // When/Then
        assertEquals(SOTA_API_SOURCE, sotaClient.getSourceName());
    }

    @Test
    void shouldHandle_InvalidFrequencyFormat() {
        // Given: Invalid frequency value
        Instant recentTime = Instant.now().minus(5, ChronoUnit.MINUTES);

        String jsonResponse = String.format("""
            [
              {
                "id": 123456,
                "activatorCallsign": "W1ABC/P",
                "summitCode": "W7W/LC-001",
                "summitDetails": "Mount Test",
                "frequency": "not-a-number",
                "mode": "SSB",
                "timeStamp": "%s"
              }
            ]
            """, recentTime.truncatedTo(ChronoUnit.SECONDS).toString().replace("Z", ""));

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = sotaClient.fetch();

        // Then: Should handle gracefully with null frequency
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).frequency());
    }

    @Test
    void shouldTrack_FreshnessAfterSuccessfulFetch() {
        // Given: Valid response with recent spot
        Instant recentTime = Instant.now().minus(5, ChronoUnit.MINUTES);

        String jsonResponse = String.format("""
            [
              {
                "id": 123456,
                "activatorCallsign": "W1ABC/P",
                "summitCode": "W7W/LC-001",
                "summitDetails": "Mount Test",
                "frequency": "14.250",
                "mode": "SSB",
                "timeStamp": "%s"
              }
            ]
            """, recentTime.truncatedTo(ChronoUnit.SECONDS).toString().replace("Z", ""));

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody(jsonResponse)));

        // When: Fetch activations
        sotaClient.fetch();

        // Then: Freshness tracking should be updated
        assertNotNull(sotaClient.getLastSuccessfulRefresh());
        assertFalse(sotaClient.isServingStaleData());
        assertNotNull(sotaClient.getDataAge());
    }

    @Test
    void shouldTrack_StaleDataAfterFailedFetch() {
        // Given: API returns error
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // When: Fetch activations
        sotaClient.fetch();

        // Then: Should be serving stale/default data
        assertTrue(sotaClient.isServingStaleData());
    }

    /**
     * Stub subclass that allows URL override for testing.
     */
    static class StubSotaClient extends SotaClient {
        StubSotaClient(
                WebClient.Builder webClientBuilder,
                CacheManager cacheManager,
                CircuitBreakerRegistry circuitBreakerRegistry,
                RetryRegistry retryRegistry,
                String testUrl) {
            super(webClientBuilder, cacheManager, circuitBreakerRegistry, retryRegistry, testUrl);
        }
    }
}
