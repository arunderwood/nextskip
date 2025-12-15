package io.nextskip.activations.internal;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.nextskip.activations.internal.dto.PotaSpotDto;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.common.client.ExternalDataClient;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
            Instant spottedAt = parseTimestamp(dto.spotTime());

            // Parse frequency (kHz string to Double)
            Double frequency = null;
            if (dto.frequency() != null && !dto.frequency().isBlank()) {
                try {
                    frequency = Double.parseDouble(dto.frequency());
                } catch (NumberFormatException e) {
                    LOG.debug("Invalid frequency format: {}", dto.frequency());
                }
            }

            // Parse coordinates
            Double latitude = parseDouble(dto.latitude());
            Double longitude = parseDouble(dto.longitude());

            return new Activation(
                    dto.spotId() != null ? dto.spotId().toString() : null,
                    dto.activator(),
                    dto.reference(),
                    dto.name(),
                    ActivationType.POTA,
                    frequency,
                    dto.mode(),
                    dto.grid6(),
                    latitude,
                    longitude,
                    spottedAt,
                    dto.qsos(),
                    getSourceName()
            );

        } catch (Exception e) {
            LOG.warn("Error converting POTA spot to activation: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Safely parse a string to Double.
     */
    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parse POTA timestamp format.
     *
     * <p>POTA uses "yyyy-MM-dd'T'HH:mm:ss" format without timezone suffix.
     * Assumes UTC timezone.
     */
    private Instant parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return Instant.now();
        }

        try {
            // Try parsing with 'Z' suffix first (ISO-8601)
            return Instant.parse(timestamp);
        } catch (java.time.format.DateTimeParseException e1) {
            try {
                // Try parsing without timezone (assume UTC) - POTA's actual format
                java.time.format.DateTimeFormatter formatter =
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(timestamp, formatter);
                return dateTime.toInstant(java.time.ZoneOffset.UTC);
            } catch (java.time.format.DateTimeParseException e2) {
                LOG.warn("Unable to parse timestamp from POTA: '{}', using current time", timestamp);
                return Instant.now();
            }
        }
    }
}
