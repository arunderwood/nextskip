package io.nextskip.activations.internal;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.propagation.internal.ExternalApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PotaClient using WireMock to simulate the POTA API.
 */
class PotaClientTest {

    private WireMockServer wireMockServer;
    private PotaClient potaClient;
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // Start WireMock server
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();

        // Configure client to use WireMock server
        String baseUrl = wireMockServer.baseUrl();
        WebClient.Builder webClientBuilder = WebClient.builder();
        cacheManager = new ConcurrentMapCacheManager("potaActivations");

        potaClient = new PotaClient(webClientBuilder, cacheManager, baseUrl);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void shouldFetchPotaActivationsSuccessfully() {
        // Given: Mock POTA API response with valid data
        String jsonResponse = """
            [
              {
                "spotId": 123456,
                "activator": "W1ABC",
                "reference": "US-0001",
                "name": "Test Park",
                "frequency": "14250",
                "mode": "SSB",
                "grid6": "FN42",
                "latitude": "42.5",
                "longitude": "-71.3",
                "spotTime": "2025-12-14T12:30:00",
                "qsos": 15
              },
              {
                "spotId": 123457,
                "activator": "K2DEF",
                "reference": "US-0002",
                "name": "Another Park",
                "frequency": "7200",
                "mode": "CW",
                "grid6": "FN31",
                "latitude": "41.5",
                "longitude": "-72.3",
                "spotTime": "2025-12-14T12:35:00Z",
                "qsos": 8
              }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = potaClient.fetch();

        // Then: Should parse and return activations
        assertNotNull(result);
        assertEquals(2, result.size());

        Activation first = result.get(0);
        assertEquals("123456", first.spotId());
        assertEquals("W1ABC", first.activatorCallsign());
        assertEquals("US-0001", first.reference());
        assertEquals("Test Park", first.referenceName());
        assertEquals(ActivationType.POTA, first.type());
        assertEquals(14250.0, first.frequency());
        assertEquals("SSB", first.mode());
        assertEquals("FN42", first.grid());
        assertEquals(42.5, first.latitude());
        assertEquals(-71.3, first.longitude());
        assertEquals(15, first.qsoCount());
        assertEquals("POTA API", first.source());
        assertNotNull(first.spottedAt());

        Activation second = result.get(1);
        assertEquals("123457", second.spotId());
        assertEquals("K2DEF", second.activatorCallsign());
    }

    @Test
    void shouldHandleEmptyResponseGracefully() {
        // Given: Empty array response
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        // When: Fetch activations
        List<Activation> result = potaClient.fetch();

        // Then: Should return empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandleNullFieldsGracefully() {
        // Given: Response with null/missing fields
        String jsonResponse = """
            [
              {
                "spotId": 123456,
                "activator": "W1ABC",
                "reference": "US-0001",
                "name": "Test Park",
                "frequency": null,
                "mode": "SSB",
                "grid6": null,
                "latitude": null,
                "longitude": null,
                "spotTime": "2025-12-14T12:30:00",
                "qsos": null
              }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = potaClient.fetch();

        // Then: Should parse successfully with null fields
        assertNotNull(result);
        assertEquals(1, result.size());

        Activation activation = result.get(0);
        assertEquals("W1ABC", activation.activatorCallsign());
        assertNull(activation.frequency());
        assertNull(activation.grid());
        assertNull(activation.latitude());
        assertNull(activation.longitude());
        assertNull(activation.qsoCount());
    }

    @Test
    void shouldHandleHttpErrorWithException() {
        // Given: API returns 500 error
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // When/Then: Should throw ExternalApiException
        ExternalApiException exception = assertThrows(ExternalApiException.class,
                () -> potaClient.fetch());

        assertTrue(exception.getMessage().contains("HTTP 500"));
        assertTrue(exception.getMessage().contains("POTA API"));
    }

    @Test
    void shouldHandleNetworkErrorWithException() {
        // Given: Simulate network error by not stubbing any response
        wireMockServer.stop();

        // When/Then: Should throw ExternalApiException
        ExternalApiException exception = assertThrows(ExternalApiException.class,
                () -> potaClient.fetch());

        assertTrue(exception.getMessage().contains("Network error") ||
                exception.getMessage().contains("Connection refused"));
    }

    @Test
    void shouldParseTimestampWithoutZSuffix() {
        // Given: POTA format without 'Z' suffix
        String jsonResponse = """
            [
              {
                "spotId": 123456,
                "activator": "W1ABC",
                "reference": "US-0001",
                "name": "Test Park",
                "frequency": "14250",
                "mode": "SSB",
                "spotTime": "2025-12-14T12:30:00"
              }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = potaClient.fetch();

        // Then: Should parse timestamp correctly
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).spottedAt());
    }

    @Test
    void shouldParseTimestampWithZSuffix() {
        // Given: ISO-8601 format with 'Z' suffix
        String jsonResponse = """
            [
              {
                "spotId": 123456,
                "activator": "W1ABC",
                "reference": "US-0001",
                "name": "Test Park",
                "frequency": "14250",
                "mode": "SSB",
                "spotTime": "2025-12-14T12:30:00Z"
              }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = potaClient.fetch();

        // Then: Should parse timestamp correctly
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).spottedAt());
    }

    @Test
    void shouldHandleInvalidTimestampGracefully() {
        // Given: Invalid timestamp format
        String jsonResponse = """
            [
              {
                "spotId": 123456,
                "activator": "W1ABC",
                "reference": "US-0001",
                "name": "Test Park",
                "frequency": "14250",
                "mode": "SSB",
                "spotTime": "invalid-timestamp"
              }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = potaClient.fetch();

        // Then: Should use current time as fallback
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).spottedAt());

        // Timestamp should be recent (within last minute)
        Instant now = Instant.now();
        Instant spottedAt = result.get(0).spottedAt();
        assertTrue(spottedAt.isAfter(now.minusSeconds(60)));
    }

    @Test
    void shouldReturnSourceName() {
        // When/Then
        assertEquals("POTA API", potaClient.getSourceName());
    }

    @Test
    void shouldHandleInvalidFrequencyFormat() {
        // Given: Invalid frequency value
        String jsonResponse = """
            [
              {
                "spotId": 123456,
                "activator": "W1ABC",
                "reference": "US-0001",
                "name": "Test Park",
                "frequency": "not-a-number",
                "mode": "SSB",
                "spotTime": "2025-12-14T12:30:00"
              }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = potaClient.fetch();

        // Then: Should handle gracefully with null frequency
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).frequency());
    }
}
