package io.nextskip.activations.internal;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.nextskip.activations.internal.dto.SotaSpotDto;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.activations.model.Summit;
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
 * Client for SOTA (Summits on the Air) API.
 *
 * <p>Fetches recent SOTA activations from https://api2.sota.org.uk/api/spots/50
 *
 * <p>Note: SOTA API returns the last 50 spots (historical), so we filter to
 * only spots from the last 45 minutes to show current activations.
 *
 * <p>Features:
 * <ul>
 *   <li>Circuit breaker to prevent cascading failures</li>
 *   <li>Retry logic for transient failures</li>
 *   <li>60-second cache TTL for real-time data</li>
 *   <li>Fallback to cached data on failures</li>
 *   <li>Filters to recent spots only (last 45 minutes)</li>
 * </ul>
 */
@Component
public class SotaClient implements ExternalDataClient<List<Activation>> {

    private static final Logger LOG = LoggerFactory.getLogger(SotaClient.class);

    private static final String SOTA_URL = "https://api2.sota.org.uk/api/spots/50";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration RECENCY_THRESHOLD = Duration.ofMinutes(45);

    private final WebClient webClient;
    private final CacheManager cacheManager;

    @org.springframework.beans.factory.annotation.Autowired
    public SotaClient(WebClient.Builder webClientBuilder, CacheManager cacheManager) {
        this(webClientBuilder, cacheManager, SOTA_URL);
    }

    protected SotaClient(WebClient.Builder webClientBuilder, CacheManager cacheManager, String baseUrl) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(2 * 1024 * 1024)) // 2MB limit for spot lists
                .build();
        this.cacheManager = cacheManager;
    }

    @Override
    public String getSourceName() {
        return "SOTA API";
    }

    /**
     * Fetch recent SOTA activations (last 45 minutes only).
     *
     * @return List of recent SOTA spots, or empty list if unavailable
     * @throws ExternalApiException if the API call fails
     */
    @Override
    @CircuitBreaker(name = "sota", fallbackMethod = "getCachedActivations")
    @Retry(name = "sota")
    @Cacheable(value = "sotaActivations", key = "'current'", unless = "#result == null || #result.isEmpty()")
    public List<Activation> fetch() {
        LOG.debug("Fetching SOTA activations from API");

        try {
            List<SotaSpotDto> spots = webClient.get()
                    .retrieve()
                    .bodyToFlux(SotaSpotDto.class)
                    .timeout(REQUEST_TIMEOUT)
                    .collectList()
                    .block();

            if (spots == null) {
                LOG.warn("No data received from SOTA API");
                return List.of();
            }

            Instant cutoff = Instant.now().minus(RECENCY_THRESHOLD);

            List<Activation> activations = spots.stream()
                    .map(this::toActivation)
                    .filter(a -> a != null)
                    .filter(a -> a.spottedAt() != null && a.spottedAt().isAfter(cutoff))
                    .toList();

            LOG.info("Successfully fetched {} recent SOTA activations (filtered from {} total spots)",
                    activations.size(), spots.size());
            return activations;

        } catch (WebClientResponseException e) {
            LOG.error("HTTP error from SOTA API: {} {}", e.getStatusCode(), e.getStatusText());
            throw new ExternalApiException("SOTA",
                    "HTTP " + e.getStatusCode() + " from SOTA API: " + e.getStatusText(), e);

        } catch (WebClientRequestException e) {
            LOG.error("Network error connecting to SOTA API", e);
            throw new ExternalApiException("SOTA",
                    "Network error connecting to SOTA API: " + e.getMessage(), e);

        } catch (Exception e) {
            LOG.error("Unexpected error fetching SOTA activations", e);
            throw new ExternalApiException("SOTA",
                    "Unexpected error fetching SOTA data: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method when SOTA service is unavailable.
     * Returns cached data if available, or empty list.
     */
    @SuppressWarnings("unused")
    private List<Activation> getCachedActivations(Exception e) {
        LOG.warn("Using cached SOTA activations due to: {}", e.getMessage());

        var cache = cacheManager.getCache("sotaActivations");
        if (cache != null) {
            var cached = cache.get("current");
            if (cached != null && cached.get() instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Activation> result = (List<Activation>) cached.get();
                LOG.info("Returning {} cached SOTA activations", result.size());
                return result;
            }
        }

        LOG.warn("No cached SOTA activations available, returning empty list");
        return List.of();
    }

    /**
     * Convert SOTA API DTO to domain Activation model.
     */
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
                    null, // SOTA doesn't provide QSO count in spots
                    getSourceName(),
                    summit
            );

        } catch (Exception e) {
            LOG.warn("Error converting SOTA spot to activation: {}", e.getMessage());
            return null;
        }
    }
}
