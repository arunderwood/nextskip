package io.nextskip.activations.internal;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.nextskip.activations.internal.dto.SotaSpotDto;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.activations.model.Summit;
import io.nextskip.common.client.AbstractExternalDataClient;
import io.nextskip.common.util.ParsingUtils;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Client for SOTA (Summits on the Air) API.
 *
 * <p>Fetches recent SOTA activations from https://api2.sota.org.uk/api/spots/50
 *
 * <p>Note: SOTA API returns the last 50 spots (historical), so we filter to
 * only spots from the last 45 minutes to show current activations.
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
public class SotaClient extends AbstractExternalDataClient<List<Activation>> {

    private static final String CLIENT_NAME = "sota";
    private static final String SOURCE_NAME = "SOTA API";
    private static final String CACHE_NAME = "sotaActivations";
    private static final String CACHE_KEY = "current";

    /**
     * Refresh interval for data fetching.
     *
     * <p>SOTA spots are real-time activation data. 1 minute refresh provides
     * timely alerts for summit activations while being respectful of the API.
     * No official rate limit documentation is available.
     *
     * @see <a href="https://www.sota.org.uk/Sota-Api/Resources">SOTA API Resources</a>
     */
    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(1);
    private static final String SOTA_URL = "https://api2.sota.org.uk/api/spots/50";
    private static final Duration RECENCY_THRESHOLD = Duration.ofMinutes(45);

    @org.springframework.beans.factory.annotation.Autowired
    public SotaClient(
            WebClient.Builder webClientBuilder,
            CacheManager cacheManager,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry) {
        this(webClientBuilder, cacheManager, circuitBreakerRegistry, retryRegistry, SOTA_URL);
    }

    protected SotaClient(
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
        getLog().debug("Fetching SOTA activations from API");

        List<SotaSpotDto> spots = getWebClient().get()
                .retrieve()
                .bodyToFlux(SotaSpotDto.class)
                .timeout(getRequestTimeout())
                .collectList()
                .block();

        if (spots == null) {
            getLog().warn("No data received from SOTA API");
            return List.of();
        }

        Instant cutoff = Instant.now().minus(RECENCY_THRESHOLD);

        List<Activation> activations = spots.stream()
                .map(this::toActivation)
                .filter(a -> a != null)
                .filter(a -> a.spottedAt() != null && a.spottedAt().isAfter(cutoff))
                .toList();

        getLog().info("Successfully fetched {} recent SOTA activations (filtered from {} total spots)",
                activations.size(), spots.size());
        return activations;
    }

    @Override
    protected List<Activation> getDefaultValue() {
        return List.of();
    }

    // ========== SOTA-specific parsing ==========

    /**
     * Convert SOTA API DTO to domain Activation model.
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException") // Intentional: return null for invalid spots
    private Activation toActivation(SotaSpotDto dto) {
        if (dto == null) {
            return null;
        }

        try {
            // Parse timestamp (SOTA format: "2024-01-15T14:30:00" - no Z suffix)
            Instant spottedAt = ParsingUtils.parseTimestamp(dto.timeStamp(), "SOTA");

            // Parse frequency (MHz string to kHz Double) - SOTA returns MHz, we need kHz
            Double frequency = ParsingUtils.parseFrequencyMhzToKhz(dto.frequency());

            // Map association code to state abbreviation using static mapper
            String regionCode = SotaAssociationMapper.toStateCode(dto.associationCode()).orElse(null);

            // Create Summit object with all location data
            Summit summit = new Summit(
                    dto.summitCode(),
                    dto.summitDetails(),
                    regionCode,
                    dto.associationCode()
            );

            return new Activation(
                    dto.id() != null ? dto.id().toString() : null,
                    dto.activatorCallsign(),
                    ActivationType.SOTA,
                    frequency,
                    dto.mode(),
                    spottedAt,
                    Instant.now(), // lastSeenAt: when we observed this activation in the API
                    null, // SOTA doesn't provide QSO count in spots
                    getSourceName(),
                    summit
            );

        } catch (Exception e) {
            getLog().warn("Error converting SOTA spot to activation: {}", e.getMessage());
            return null;
        }
    }
}
