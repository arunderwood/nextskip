package io.nextskip.activations.internal;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.nextskip.activations.internal.dto.PotaSpotDto;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.activations.model.Park;
import io.nextskip.common.client.AbstractExternalDataClient;
import io.nextskip.common.util.ParsingUtils;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Client for POTA (Parks on the Air) API.
 *
 * <p>Fetches current POTA activations from https://api.pota.app/spot/activator
 *
 * <p>Extends {@link AbstractExternalDataClient} to inherit:
 * <ul>
 *   <li>Circuit breaker to prevent cascading failures</li>
 *   <li>Retry logic for transient failures</li>
 *   <li>Cache fallback on failures</li>
 *   <li>Freshness tracking for UI display</li>
 * </ul>
 */
@Component
public class PotaClient extends AbstractExternalDataClient<List<Activation>> {

    private static final String CLIENT_NAME = "pota";
    private static final String SOURCE_NAME = "POTA API";
    private static final String CACHE_NAME = "potaActivations";
    private static final String CACHE_KEY = "current";

    /**
     * Refresh interval for data fetching.
     *
     * <p>POTA spots are real-time activation data. 1 minute refresh provides
     * timely alerts for park activations while being respectful of the API.
     * No official rate limit documentation is available.
     */
    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(1);
    private static final String POTA_URL = "https://api.pota.app/spot/activator";

    @org.springframework.beans.factory.annotation.Autowired
    public PotaClient(
            WebClient.Builder webClientBuilder,
            CacheManager cacheManager,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry) {
        this(webClientBuilder, cacheManager, circuitBreakerRegistry, retryRegistry, POTA_URL);
    }

    protected PotaClient(
            WebClient.Builder webClientBuilder,
            CacheManager cacheManager,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            String baseUrl) {
        super(webClientBuilder, cacheManager, circuitBreakerRegistry, retryRegistry, baseUrl);
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
    protected String getCacheName() {
        return CACHE_NAME;
    }

    @Override
    protected String getCacheKey() {
        return CACHE_KEY;
    }

    @Override
    public Duration getRefreshInterval() {
        return REFRESH_INTERVAL;
    }

    @Override
    protected int getMaxResponseSize() {
        return 2 * 1024 * 1024; // 2MB limit for spot lists
    }

    @Override
    protected List<Activation> doFetch() {
        getLog().debug("Fetching POTA activations from API");

        List<PotaSpotDto> spots = getWebClient().get()
                .retrieve()
                .bodyToFlux(PotaSpotDto.class)
                .timeout(getRequestTimeout())
                .collectList()
                .block();

        if (spots == null) {
            getLog().warn("No data received from POTA API");
            return List.of();
        }

        List<Activation> activations = spots.stream()
                .map(this::toActivation)
                .filter(a -> a != null)
                .toList();

        getLog().info("Successfully fetched {} POTA activations", activations.size());
        return activations;
    }

    @Override
    protected List<Activation> getDefaultValue() {
        return List.of();
    }

    // ========== POTA-specific parsing ==========

    /**
     * Convert POTA API DTO to domain Activation model.
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException") // Intentional: return null for invalid spots
    private Activation toActivation(PotaSpotDto dto) {
        if (dto == null) {
            return null;
        }

        try {
            // Parse timestamp (POTA returns format like "2025-12-15T04:19:19" without timezone)
            Instant spottedAt = ParsingUtils.parseTimestamp(dto.spotTime(), "POTA");

            // Parse frequency (kHz string to Double)
            Double frequency = ParsingUtils.parseDouble(dto.frequency(), "frequency");

            // Parse coordinates
            Double latitude = ParsingUtils.parseDouble(dto.latitude());
            Double longitude = ParsingUtils.parseDouble(dto.longitude());

            // Parse location info from locationDesc (e.g., "US-CO" -> countryCode="US", regionCode="CO")
            String regionCode = ParsingUtils.parseRegionCode(dto.locationDesc());
            String countryCode = ParsingUtils.parseCountryCode(dto.locationDesc());

            // Create Park object with all location data
            Park park = new Park(
                    dto.reference(),
                    dto.name(),
                    regionCode,
                    countryCode,
                    dto.grid6(),
                    latitude,
                    longitude
            );

            return new Activation(
                    dto.spotId() != null ? dto.spotId().toString() : null,
                    dto.activator(),
                    ActivationType.POTA,
                    frequency,
                    dto.mode(),
                    spottedAt,
                    dto.qsos(),
                    getSourceName(),
                    park
            );

        } catch (Exception e) {
            getLog().warn("Error converting POTA spot to activation: {}", e.getMessage());
            return null;
        }
    }
}
