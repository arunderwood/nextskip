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

    private static final String POTA_API_SOURCE = "POTA API";
    private static final String W1ABC_CALLSIGN = "W1ABC";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_JSON = "application/json";

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
    void shouldFetch_PotaActivationsSuccessfully() {
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
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = potaClient.fetch();

        // Then: Should parse and return activations
        assertNotNull(result);
        assertEquals(2, result.size());

        Activation first = result.get(0);
        assertEquals("123456", first.spotId());
        assertEquals(W1ABC_CALLSIGN, first.activatorCallsign());
        assertEquals(ActivationType.POTA, first.type());
        assertEquals(14250.0, first.frequency());
        assertEquals("SSB", first.mode());
        assertEquals(15, first.qsoCount());
        assertEquals(POTA_API_SOURCE, first.source());
        assertNotNull(first.spottedAt());

        // Verify location data (Park object)
        assertNotNull(first.location());
        assertEquals("US-0001", first.location().reference());
        assertEquals("Test Park", first.location().name());
        assertEquals("FN42", ((io.nextskip.activations.model.Park) first.location()).grid());
        assertEquals(42.5, ((io.nextskip.activations.model.Park) first.location()).latitude());
        assertEquals(-71.3, ((io.nextskip.activations.model.Park) first.location()).longitude());

        Activation second = result.get(1);
        assertEquals("123457", second.spotId());
        assertEquals("K2DEF", second.activatorCallsign());
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
        List<Activation> result = potaClient.fetch();

        // Then: Should return empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandle_NullFieldsGracefully() {
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
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = potaClient.fetch();

        // Then: Should parse successfully with null fields
        assertNotNull(result);
        assertEquals(1, result.size());

        Activation activation = result.get(0);
        assertEquals(W1ABC_CALLSIGN, activation.activatorCallsign());
        assertNull(activation.frequency());
        assertNull(activation.qsoCount());

        // Verify location object exists but has null coordinate/grid fields
        assertNotNull(activation.location());
        assertNull(((io.nextskip.activations.model.Park) activation.location()).grid());
        assertNull(((io.nextskip.activations.model.Park) activation.location()).latitude());
        assertNull(((io.nextskip.activations.model.Park) activation.location()).longitude());
    }

    @Test
    void shouldHandle_HttpErrorWithException() {
        // Given: API returns 500 error
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // When/Then: Should throw ExternalApiException
        ExternalApiException exception = assertThrows(ExternalApiException.class,
                () -> potaClient.fetch());

        assertTrue(exception.getMessage().contains("HTTP 500"));
        assertTrue(exception.getMessage().contains(POTA_API_SOURCE));
    }

    @Test
    void shouldHandle_NetworkErrorWithException() {
        // Given: Simulate network error by not stubbing any response
        wireMockServer.stop();

        // When/Then: Should throw ExternalApiException
        ExternalApiException exception = assertThrows(ExternalApiException.class,
                () -> potaClient.fetch());

        assertTrue(exception.getMessage().contains("Network error") ||
                exception.getMessage().contains("Connection refused"));
    }

    @Test
    void shouldParse_TimestampWithoutZSuffix() {
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
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = potaClient.fetch();

        // Then: Should parse timestamp correctly
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).spottedAt());
    }

    @Test
    void shouldParse_TimestampWithZSuffix() {
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
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = potaClient.fetch();

        // Then: Should parse timestamp correctly
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).spottedAt());
    }

    @Test
    void shouldHandle_InvalidTimestampGracefully() {
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
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
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
    void shouldReturn_SourceName() {
        // When/Then
        assertEquals(POTA_API_SOURCE, potaClient.getSourceName());
    }

    @Test
    void shouldHandle_InvalidFrequencyFormat() {
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
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = potaClient.fetch();

        // Then: Should handle gracefully with null frequency
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).frequency());
    }
}
