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
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SotaClient using WireMock to simulate the SOTA API.
 */
class SotaClientTest {

    private WireMockServer wireMockServer;
    private SotaClient sotaClient;
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // Start WireMock server
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();

        // Configure client to use WireMock server
        String baseUrl = wireMockServer.baseUrl();
        WebClient.Builder webClientBuilder = WebClient.builder();
        cacheManager = new ConcurrentMapCacheManager("sotaActivations");

        sotaClient = new SotaClient(webClientBuilder, cacheManager, baseUrl);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void shouldFetchRecentSotaActivationsSuccessfully() {
        // Given: Mock SOTA API response with recent and old spots
        Instant now = Instant.now();
        Instant recentTime = now.minus(5, ChronoUnit.MINUTES);
        Instant oldTime = now.minus(45, ChronoUnit.MINUTES);

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
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = sotaClient.fetch();

        // Then: Should return only recent activation (within 30 minutes)
        assertNotNull(result);
        assertEquals(1, result.size(), "Should filter to only recent spots");

        Activation activation = result.get(0);
        assertEquals("123456", activation.spotId());
        assertEquals("W1ABC/P", activation.activatorCallsign());
        assertEquals(ActivationType.SOTA, activation.type());
        assertEquals(14250.0, activation.frequency(), "Frequency should be converted from MHz to kHz");
        assertEquals("SSB", activation.mode());
        assertNull(activation.qsoCount(), "SOTA doesn't provide QSO count");
        assertEquals("SOTA API", activation.source());
        assertNotNull(activation.spottedAt());

        // Verify location data (Summit object)
        assertNotNull(activation.location());
        assertEquals("W7W/LC-001", activation.location().reference());
        assertEquals("Mount Test", activation.location().name());
        assertEquals("WA", activation.location().regionCode(), "Should map W7W association to WA state");
        assertEquals("W7W", ((io.nextskip.activations.model.Summit) activation.location()).associationCode());
    }

    @Test
    void shouldFilterOutOldSpots() {
        // Given: All spots are older than 30 minutes
        Instant oldTime = Instant.now().minus(45, ChronoUnit.MINUTES);

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
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = sotaClient.fetch();

        // Then: Should return empty list
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Should filter out spots older than 30 minutes");
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
        List<Activation> result = sotaClient.fetch();

        // Then: Should return empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandleNullFieldsGracefully() {
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
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = sotaClient.fetch();

        // Then: Should parse successfully with null fields
        assertNotNull(result);
        assertEquals(1, result.size());

        Activation activation = result.get(0);
        assertEquals("W1ABC/P", activation.activatorCallsign());
        assertNull(activation.frequency());
        assertNull(activation.mode());
    }

    @Test
    void shouldConvertFrequencyFromMHzToKHz() {
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
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = sotaClient.fetch();

        // Then: Should convert MHz to kHz
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(3573.0, result.get(0).frequency(), 0.01);
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
                () -> sotaClient.fetch());

        assertTrue(exception.getMessage().contains("HTTP 500"));
        assertTrue(exception.getMessage().contains("SOTA API"));
    }

    @Test
    void shouldHandleNetworkErrorWithException() {
        // Given: Simulate network error by stopping server
        wireMockServer.stop();

        // When/Then: Should throw ExternalApiException
        ExternalApiException exception = assertThrows(ExternalApiException.class,
                () -> sotaClient.fetch());

        assertTrue(exception.getMessage().contains("Network error") ||
                exception.getMessage().contains("Connection refused"));
    }

    @Test
    void shouldParseTimestampWithoutZSuffix() {
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
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = sotaClient.fetch();

        // Then: Should parse timestamp correctly
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).spottedAt());
    }

    @Test
    void shouldParseTimestampWithZSuffix() {
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
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = sotaClient.fetch();

        // Then: Should parse timestamp correctly
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).spottedAt());
    }

    @Test
    void shouldHandleInvalidTimestampGracefully() {
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
                        .withHeader("Content-Type", "application/json")
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
    void shouldReturnSourceName() {
        // When/Then
        assertEquals("SOTA API", sotaClient.getSourceName());
    }

    @Test
    void shouldHandleInvalidFrequencyFormat() {
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
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        // When: Fetch activations
        List<Activation> result = sotaClient.fetch();

        // Then: Should handle gracefully with null frequency
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).frequency());
    }
}
