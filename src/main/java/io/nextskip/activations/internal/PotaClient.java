package io.nextskip.activations.internal;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.nextskip.activations.internal.dto.PotaSpotDto;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.activations.model.Park;
import io.nextskip.common.client.ExternalDataClient;
import io.nextskip.common.util.ParsingUtils;
import io.nextskip.propagation.internal.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Client for POTA (Parks on the Air) API.
 *
 * <p>Fetches current POTA activations from https://api.pota.app/spot/activator
 *
 * <p>Features:
 * <ul>
 *   <li>Circuit breaker to prevent cascading failures</li>
 *   <li>Retry logic for transient failures</li>
 *   <li>60-second cache TTL for real-time data</li>
 *   <li>Fallback to cached data on failures</li>
 * </ul>
 */
@Component
public class PotaClient implements ExternalDataClient<List<Activation>> {

    private static final Logger LOG = LoggerFactory.getLogger(PotaClient.class);

    private static final String POTA_URL = "https://api.pota.app/spot/activator";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;
    private final CacheManager cacheManager;

    @org.springframework.beans.factory.annotation.Autowired
    public PotaClient(WebClient.Builder webClientBuilder, CacheManager cacheManager) {
        this(webClientBuilder, cacheManager, POTA_URL);
    }

    protected PotaClient(WebClient.Builder webClientBuilder, CacheManager cacheManager, String baseUrl) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(2 * 1024 * 1024)) // 2MB limit for spot lists
                .build();
        this.cacheManager = cacheManager;
    }

    @Override
    public String getSourceName() {
        return "POTA API";
    }

    /**
     * Fetch current POTA activations.
     *
     * @return List of active POTA spots, or empty list if unavailable
     * @throws ExternalApiException if the API call fails
     */
    @Override
    @CircuitBreaker(name = "pota", fallbackMethod = "getCachedActivations")
    @Retry(name = "pota")
    @Cacheable(value = "potaActivations", key = "'current'", unless = "#result == null || #result.isEmpty()")
    public List<Activation> fetch() {
        LOG.debug("Fetching POTA activations from API");

        try {
            List<PotaSpotDto> spots = webClient.get()
                    .retrieve()
                    .bodyToFlux(PotaSpotDto.class)
                    .timeout(REQUEST_TIMEOUT)
                    .collectList()
                    .block();

            if (spots == null) {
                LOG.warn("No data received from POTA API");
                return List.of();
            }

            List<Activation> activations = spots.stream()
                    .map(this::toActivation)
                    .filter(a -> a != null)
                    .toList();

            LOG.info("Successfully fetched {} POTA activations", activations.size());
            return activations;

        } catch (WebClientResponseException e) {
            LOG.error("HTTP error from POTA API: {} {}", e.getStatusCode(), e.getStatusText());
            throw new ExternalApiException("POTA",
                    "HTTP " + e.getStatusCode() + " from POTA API: " + e.getStatusText(), e);

        } catch (WebClientRequestException e) {
            LOG.error("Network error connecting to POTA API", e);
            throw new ExternalApiException("POTA",
                    "Network error connecting to POTA API: " + e.getMessage(), e);

        } catch (Exception e) {
            LOG.error("Unexpected error fetching POTA activations", e);
            throw new ExternalApiException("POTA",
                    "Unexpected error fetching POTA data: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method when POTA service is unavailable.
     * Returns cached data if available, or empty list.
     */
    @SuppressWarnings("unused")
    private List<Activation> getCachedActivations(Exception e) {
        LOG.warn("Using cached POTA activations due to: {}", e.getMessage());

        var cache = cacheManager.getCache("potaActivations");
        if (cache != null) {
            var cached = cache.get("current");
            if (cached != null && cached.get() instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Activation> result = (List<Activation>) cached.get();
                LOG.info("Returning {} cached POTA activations", result.size());
                return result;
            }
        }

        LOG.warn("No cached POTA activations available, returning empty list");
        return List.of();
    }

    /**
     * Convert POTA API DTO to domain Activation model.
     */
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
            LOG.warn("Error converting POTA spot to activation: {}", e.getMessage());
            return null;
        }
    }
}
