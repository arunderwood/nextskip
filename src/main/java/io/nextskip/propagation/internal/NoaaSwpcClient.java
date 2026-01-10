package io.nextskip.propagation.internal;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.nextskip.common.client.AbstractExternalDataClient;
import io.nextskip.common.client.InvalidApiResponseException;
import io.nextskip.propagation.model.SolarIndices;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

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
 * <p>Fetches current solar indices (SFI, sunspot number) from NOAA's
 * observed solar cycle indices JSON endpoint.
 *
 * <p>Extends {@link AbstractExternalDataClient} to inherit:
 * <ul>
 *   <li>Circuit breaker to prevent cascading failures</li>
 *   <li>Retry logic for transient failures</li>
 *   <li>Freshness tracking for UI display</li>
 * </ul>
 */
@Component
public class NoaaSwpcClient extends AbstractExternalDataClient<SolarIndices> {

    private static final String CLIENT_NAME = "noaa";
    private static final String SOURCE_NAME = "NOAA";

    /**
     * Refresh interval for data fetching.
     *
     * <p>NOAA solar cycle data updates approximately daily. A 30-minute interval
     * balances data freshness with bandwidth efficiency.
     *
     * @see <a href="https://www.swpc.noaa.gov/content/data-access">NOAA SWPC Data Access</a>
     */
    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(30);
    private static final String NOAA_URL =
            "https://services.swpc.noaa.gov/json/solar-cycle/observed-solar-cycle-indices.json";

    @org.springframework.beans.factory.annotation.Autowired
    public NoaaSwpcClient(
            WebClient.Builder webClientBuilder,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry) {
        this(webClientBuilder, circuitBreakerRegistry, retryRegistry, NOAA_URL);
    }

    protected NoaaSwpcClient(
            WebClient.Builder webClientBuilder,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            String baseUrl) {
        super(webClientBuilder, circuitBreakerRegistry, retryRegistry, baseUrl);
    }

    // ========== AbstractExternalDataClient implementation ==========

    @Override
    protected String getClientName() {
        return CLIENT_NAME;
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    @Override
    public Duration getRefreshInterval() {
        return REFRESH_INTERVAL;
    }

    @Override
    protected SolarIndices doFetch() {
        getLog().debug("Fetching solar indices from NOAA SWPC");

        List<NoaaSolarCycleEntry> data = getWebClient().get()
                .retrieve()
                .bodyToFlux(NoaaSolarCycleEntry.class)
                .timeout(getRequestTimeout())
                .collectList()
                .block();

        if (data == null || data.isEmpty()) {
            getLog().warn("No data received from NOAA SWPC");
            throw new InvalidApiResponseException(SOURCE_NAME, "Empty response from NOAA API");
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

        getLog().info("Successfully fetched solar indices: SFI={}, Sunspots={}",
                latest.solarFlux(), latest.sunspotNumber());
        return indices;
    }

    // ========== NOAA-specific parsing ==========

    /**
     * Parse timestamp from NOAA API, supporting various formats.
     *
     * <p>NOAA sometimes returns partial dates like "2025-11" (year-month only).
     * This method handles full ISO-8601 instants, partial dates, and falls back
     * to current time if parsing fails.
     *
     * @param timeTag the timestamp string from the API
     * @return parsed Instant, or current time if parsing fails
     */
    private Instant parseTimestamp(String timeTag) {
        if (timeTag == null || timeTag.isBlank()) {
            getLog().warn("Missing timestamp from NOAA, using current time");
            return Instant.now();
        }

        try {
            // Try parsing as full ISO-8601 instant first
            return Instant.parse(timeTag);
        } catch (DateTimeParseException e) {
            // Try parsing as partial date (e.g., "2025-11")
            try {
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
                getLog().warn("Unable to parse timestamp from NOAA: '{}', using current time", timeTag);
                return Instant.now();
            }
        }
    }
}
