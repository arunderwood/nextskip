package io.nextskip.activations.internal;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.nextskip.activations.model.Activation;
import io.nextskip.test.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for cache behavior.
 *
 * <p>Verifies cache-as-fallback behavior:
 * <ul>
 *   <li>Cache is populated after successful API calls</li>
 *   <li>Cache is used as fallback when API fails</li>
 *   <li>API is always called (cache is not checked first)</li>
 * </ul>
 */
@SpringBootTest
class CacheIntegrationTest extends AbstractIntegrationTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private PotaClient potaClient;

    @Autowired
    private CacheManager cacheManager;

    @TestConfiguration
    static class WireMockClientConfig {
        @Bean
        @Primary
        PotaClient wireMockPotaClient(
                WebClient.Builder webClientBuilder,
                CacheManager cacheManager,
                CircuitBreakerRegistry circuitBreakerRegistry,
                RetryRegistry retryRegistry) {
            // Access to protected constructor is available because this is in the same package
            return new PotaClient(webClientBuilder, cacheManager,
                    circuitBreakerRegistry, retryRegistry, wireMockServer.baseUrl());
        }
    }

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void resetWireMockAndCache() {
        wireMockServer.resetAll();
        // Clear cache before each test
        var cache = cacheManager.getCache("potaActivations");
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void testCaching_ApiAlwaysCalled_NotCachedFirst() {
        // Given: API returns empty array
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        // When: Fetch twice
        List<Activation> firstResult = potaClient.fetch();
        List<Activation> secondResult = potaClient.fetch();

        // Then: Both results should be empty lists
        assertNotNull(firstResult);
        assertTrue(firstResult.isEmpty());
        assertNotNull(secondResult);
        assertTrue(secondResult.isEmpty());

        // And: API should be called twice (cache is not checked first)
        wireMockServer.verify(2, getRequestedFor(urlEqualTo("/")));
    }

    @Test
    void testCaching_CacheFallback_WhenApiFailsAfterSuccess() {
        // Given: API returns data on first call
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

        // When: First fetch succeeds and populates cache
        List<Activation> firstResult = potaClient.fetch();

        // Then: First result should have 1 activation
        assertEquals(1, firstResult.size());
        assertEquals("W1ABC", firstResult.getFirst().activatorCallsign());

        // When: API starts returning errors
        wireMockServer.resetAll();
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // When: Second fetch fails, should get cached data as fallback
        List<Activation> fallbackResult = potaClient.fetch();

        // Then: Should return cached data
        assertNotNull(fallbackResult);
        assertEquals(1, fallbackResult.size());
        assertEquals("W1ABC", fallbackResult.getFirst().activatorCallsign());
        assertTrue(potaClient.isServingStaleData());
    }

    @Test
    void testCaching_CacheIsPopulated_AfterFetch() {
        // Given: API returns data
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        // When: Fetch once
        potaClient.fetch();

        // Then: Cache should contain the result
        var cache = cacheManager.getCache("potaActivations");
        assertNotNull(cache, "potaActivations cache should exist");
        assertNotNull(cache.get("current"), "Cache entry 'current' should exist after fetch");
    }
}
