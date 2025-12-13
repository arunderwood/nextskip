package io.nextskip.propagation.internal;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.nextskip.propagation.model.SolarIndices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Client for NOAA Space Weather Prediction Center (SWPC) solar data.
 *
 * Fetches current solar indices (SFI, sunspot number) from NOAA's
 * observed solar cycle indices JSON endpoint.
 *
 * Features:
 * - Circuit breaker to prevent cascading failures
 * - Retry logic for transient failures
 * - 5-minute cache TTL
 * - Fallback to cached data on failures
 */
@Component
public class NoaaSwpcClient {

    private static final Logger log = LoggerFactory.getLogger(NoaaSwpcClient.class);

    private static final String NOAA_URL =
            "https://services.swpc.noaa.gov/json/solar-cycle/observed-solar-cycle-indices.json";

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;
    private final CacheManager cacheManager;

    @org.springframework.beans.factory.annotation.Autowired
    public NoaaSwpcClient(WebClient.Builder webClientBuilder, CacheManager cacheManager) {
        this(webClientBuilder, cacheManager, NOAA_URL);
    }

    protected NoaaSwpcClient(WebClient.Builder webClientBuilder, CacheManager cacheManager, String baseUrl) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(1024 * 1024)) // 1MB limit
                .build();
        this.cacheManager = cacheManager;
    }

    /**
     * Fetch current solar indices from NOAA SWPC.
     *
     * @return SolarIndices with current data, or null if unavailable
     * @throws ExternalApiException if the API call fails
     * @throws InvalidApiResponseException if the response is invalid
     */
    @CircuitBreaker(name = "noaa", fallbackMethod = "getCachedIndices")
    @Retry(name = "noaa")
    @Cacheable(value = "solarIndices", key = "'latest'", unless = "#result == null")
    public SolarIndices fetchSolarIndices() {
        log.debug("Fetching solar indices from NOAA SWPC");

        try {
            // Fetch data with type-safe DTO
            List<NoaaSolarCycleEntry> data = webClient.get()
                    .retrieve()
                    .bodyToFlux(NoaaSolarCycleEntry.class)
                    .timeout(REQUEST_TIMEOUT)
                    .collectList()
                    .block();

            if (data == null || data.isEmpty()) {
                log.warn("No data received from NOAA SWPC");
                throw new InvalidApiResponseException("NOAA", "Empty response from NOAA API");
            }

            // Get the most recent observation (last entry)
            NoaaSolarCycleEntry latest = data.get(data.size() - 1);

            // Validate the data
            latest.validate();

            // NOAA doesn't provide K/A index in this endpoint,
            // so we'll use default values (fetched from HamQSL later)
            int kIndex = 0;  // Will be overridden by HamQSL data
            int aIndex = 0;  // Will be overridden by HamQSL data

            // Parse timestamp with support for partial dates (e.g., "2025-11")
            Instant timestamp = parseTimestamp(latest.timeTag());

            SolarIndices indices = new SolarIndices(
                    latest.solarFlux(),
                    aIndex,
                    kIndex,
                    latest.sunspotNumber(),
                    timestamp,
                    "NOAA SWPC"
            );

            log.info("Successfully fetched solar indices: SFI={}, Sunspots={}",
                    latest.solarFlux(), latest.sunspotNumber());
            return indices;

        } catch (WebClientResponseException e) {
            // HTTP error (4xx, 5xx)
            log.error("HTTP error from NOAA API: {} {}", e.getStatusCode(), e.getStatusText());
            throw new ExternalApiException("NOAA",
                    "HTTP " + e.getStatusCode() + " from NOAA API: " + e.getStatusText(), e);

        } catch (WebClientRequestException e) {
            // Network error (connection refused, timeout, etc.)
            log.error("Network error connecting to NOAA API", e);
            throw new ExternalApiException("NOAA",
                    "Network error connecting to NOAA API: " + e.getMessage(), e);

        } catch (InvalidApiResponseException e) {
            // Validation error - just rethrow
            log.error("Invalid response from NOAA API: {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            // Unexpected error
            log.error("Unexpected error fetching solar indices from NOAA SWPC", e);
            throw new ExternalApiException("NOAA",
                    "Unexpected error fetching NOAA solar data: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method when NOAA service is unavailable.
     * Returns cached data if available.
     */
    @SuppressWarnings("unused")
    private SolarIndices getCachedIndices(Exception e) {
        log.warn("Using cached solar indices due to: {}", e.getMessage());

        var cache = cacheManager.getCache("solarIndices");
        if (cache != null) {
            var cached = cache.get("latest", SolarIndices.class);
            if (cached != null) {
                log.info("Returning cached solar indices from {}", cached.timestamp());
                return cached;
            }
        }

        log.error("No cached solar indices available");
        // Return default/degraded data rather than null
        return new SolarIndices(
                100.0,  // Default SFI
                10,     // Default A-index
                3,      // Default K-index
                50,     // Default sunspot number
                Instant.now(),
                "NOAA SWPC (Degraded)"
        );
    }

    /**
     * Parse timestamp from NOAA API, supporting various formats.
     *
     * NOAA sometimes returns partial dates like "2025-11" (year-month only).
     * This method handles full ISO-8601 instants, partial dates, and falls back
     * to current time if parsing fails.
     *
     * @param timeTag the timestamp string from the API
     * @return parsed Instant, or current time if parsing fails
     */
    private Instant parseTimestamp(String timeTag) {
        if (timeTag == null || timeTag.isBlank()) {
            log.warn("Missing timestamp from NOAA, using current time");
            return Instant.now();
        }

        try {
            // Try parsing as full ISO-8601 instant first
            return Instant.parse(timeTag);
        } catch (DateTimeParseException e) {
            // Try parsing as partial date (e.g., "2025-11")
            try {
                // Attempt to parse as LocalDate and convert to start of month
                LocalDate date;
                if (timeTag.length() == 7 && timeTag.charAt(4) == '-') {
                    // Format: "YYYY-MM"
                    date = LocalDate.parse(timeTag + "-01", DateTimeFormatter.ISO_LOCAL_DATE);
                } else {
                    // Try standard date format
                    date = LocalDate.parse(timeTag, DateTimeFormatter.ISO_LOCAL_DATE);
                }
                return date.atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (DateTimeParseException e2) {
                log.warn("Unable to parse timestamp from NOAA: '{}', using current time", timeTag);
                return Instant.now();
            }
        }
    }
}
