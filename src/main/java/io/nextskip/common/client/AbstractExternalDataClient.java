package io.nextskip.common.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * Abstract base class for external API clients providing unified resilience patterns.
 *
 * <p>This class implements the Template Method pattern, providing:
 * <ul>
 *   <li>Circuit Breaker - prevents cascading failures when external APIs are down</li>
 *   <li>Retry - handles transient failures with configurable backoff</li>
 *   <li>Cache Fallback - returns cached data when live fetch fails</li>
 *   <li>Freshness Tracking - tracks data age for UI display</li>
 * </ul>
 *
 * <p>Uses programmatic Resilience4j APIs (not annotations) to allow dynamic
 * circuit breaker names via {@link #getClientName()}.
 *
 * <p>Subclasses implement:
 * <ul>
 *   <li>{@link #getClientName()} - identifier for circuit breaker/retry instances</li>
 *   <li>{@link #getCacheName()} - Spring cache name</li>
 *   <li>{@link #getCacheKey()} - key within the cache</li>
 *   <li>{@link #doFetch()} - pure API fetch logic</li>
 *   <li>{@link #getDefaultValue()} - fallback when cache is empty</li>
 * </ul>
 *
 * @param <T> the type of data returned by this client
 */
@SuppressWarnings({
    "PMD.AvoidCatchingGenericException", // Intentional: wrap unknown exceptions
    "PMD.ConstructorCallsOverridableMethod" // Safe: getClientName/getMaxResponseSize return constants
})
public abstract class AbstractExternalDataClient<T>
        implements ExternalDataClient<T> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected final WebClient webClient;
    protected final CacheManager cacheManager;

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    // Freshness tracking
    private volatile Instant lastSuccessfulRefresh;
    private volatile boolean servingStaleData;

    /**
     * Constructs an AbstractExternalDataClient with the given dependencies.
     *
     * @param webClientBuilder builder for creating WebClient
     * @param cacheManager Spring cache manager
     * @param circuitBreakerRegistry registry for circuit breakers
     * @param retryRegistry registry for retry configurations
     * @param baseUrl the base URL for the external API
     */
    protected AbstractExternalDataClient(
            WebClient.Builder webClientBuilder,
            CacheManager cacheManager,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            String baseUrl) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(getMaxResponseSize()))
                .build();
        this.cacheManager = cacheManager;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(getClientName());
        this.retry = retryRegistry.retry(getClientName());
    }

    // ========== Abstract methods for subclasses ==========

    /**
     * Returns the client name used for circuit breaker and retry instances.
     *
     * <p>Must match an entry in application.yml resilience4j configuration.
     *
     * @return the client name (e.g., "noaa", "pota", "sota")
     */
    protected abstract String getClientName();

    /**
     * Returns the Spring cache name for storing fetched data.
     *
     * @return the cache name (e.g., "solarIndices", "potaActivations")
     */
    protected abstract String getCacheName();

    /**
     * Returns the key used within the cache.
     *
     * @return the cache key (e.g., "latest", "current")
     */
    protected abstract String getCacheKey();

    /**
     * Performs the actual API fetch.
     *
     * <p>Implementations should:
     * <ul>
     *   <li>Make the HTTP request using {@link #webClient}</li>
     *   <li>Parse the response into the target type</li>
     *   <li>Validate the response data</li>
     * </ul>
     *
     * <p>Do NOT add resilience annotations - this is handled by the base class.
     *
     * @return the fetched data
     * @throws ExternalApiException if the API call fails
     * @throws InvalidApiResponseException if the response is invalid
     */
    protected abstract T doFetch();

    /**
     * Returns a default value when both API and cache are unavailable.
     *
     * <p>Implementations can return:
     * <ul>
     *   <li>A sensible default (e.g., empty list for collections)</li>
     *   <li>Degraded data with a marker (e.g., source = "Degraded")</li>
     *   <li>{@code null} if no default makes sense</li>
     * </ul>
     *
     * @return the default value, or null
     */
    protected abstract T getDefaultValue();

    /**
     * Returns the recommended refresh interval for this data source.
     *
     * <p>Used for freshness tracking via {@link #isStale()}.
     *
     * @return the refresh interval (e.g., {@code Duration.ofMinutes(5)})
     */
    public abstract Duration getRefreshInterval();

    // ========== Configurable methods (override if needed) ==========

    /**
     * Returns the request timeout duration.
     *
     * <p>Override this method for APIs that need longer timeouts.
     *
     * @return the request timeout (default: 10 seconds)
     */
    protected Duration getRequestTimeout() {
        return Duration.ofSeconds(10);
    }

    /**
     * Returns the maximum response size in bytes.
     *
     * <p>Override for APIs that return large responses.
     *
     * @return the max response size (default: 1MB)
     */
    protected int getMaxResponseSize() {
        return 1024 * 1024; // 1MB
    }

    // ========== ExternalDataClient implementation ==========

    @Override
    @SuppressWarnings("unchecked")
    public final T fetch() {
        // Always call the API - cache is only used as fallback on failure
        // (see getCachedDataWithFallback in the catch block below)

        Supplier<T> decoratedFetch = () -> {
            try {
                T result = doFetch();

                // Update cache explicitly
                updateCache(result);

                // Update freshness tracking
                this.lastSuccessfulRefresh = Instant.now();
                this.servingStaleData = false;

                return result;

            } catch (WebClientResponseException e) {
                log.error("HTTP error from {} API: {} {}",
                        getSourceName(), e.getStatusCode(), e.getStatusText());
                throw new ExternalApiException(getClientName(),
                        "HTTP " + e.getStatusCode() + " from " + getSourceName() + ": " + e.getStatusText(), e);

            } catch (WebClientRequestException e) {
                log.error("Network error connecting to {} API", getSourceName(), e);
                throw new ExternalApiException(getClientName(),
                        "Network error connecting to " + getSourceName() + ": " + e.getMessage(), e);

            } catch (InvalidApiResponseException e) {
                log.error("Invalid response from {} API: {}", getSourceName(), e.getMessage());
                throw e;

            } catch (Exception e) {
                log.error("Unexpected error fetching from {}", getSourceName(), e);
                throw new ExternalApiException(getClientName(),
                        "Unexpected error fetching from " + getSourceName() + ": " + e.getMessage(), e);
            }
        };

        // Wrap with Retry, then Circuit Breaker
        Supplier<T> retryWrapped = Retry.decorateSupplier(retry, decoratedFetch);
        Supplier<T> cbWrapped = CircuitBreaker.decorateSupplier(circuitBreaker, retryWrapped);

        try {
            return cbWrapped.get();
        } catch (Exception e) {
            log.warn("Fetch failed for {}, attempting fallback: {}", getSourceName(), e.getMessage());
            return getFallbackData(e);
        }
    }

    // ========== Freshness tracking API ==========

    /**
     * Returns the timestamp of the last successful data refresh.
     *
     * @return the timestamp, or null if never refreshed successfully
     */
    public Instant getLastSuccessfulRefresh() {
        return lastSuccessfulRefresh;
    }

    /**
     * Returns whether the client is currently serving stale (cached) data.
     *
     * <p>True if the last fetch failed and returned cached data.
     *
     * @return true if serving stale data
     */
    public boolean isServingStaleData() {
        return servingStaleData;
    }

    /**
     * Returns the age of the current data.
     *
     * @return duration since last successful refresh, or null if never refreshed
     */
    public Duration getDataAge() {
        if (lastSuccessfulRefresh == null) {
            return null;
        }
        return Duration.between(lastSuccessfulRefresh, Instant.now());
    }

    /**
     * Returns whether the data is considered stale.
     *
     * <p>Data is stale if its age exceeds the refresh interval.
     *
     * @return true if data age exceeds refresh interval
     */
    public boolean isStale() {
        Duration age = getDataAge();
        // Never refreshed = stale; otherwise compare age to refresh interval
        return age == null || age.compareTo(getRefreshInterval()) > 0;
    }

    // ========== Helper methods ==========

    /**
     * Updates the cache with the fetched data.
     *
     * @param data the data to cache
     */
    protected void updateCache(T data) {
        if (data == null) {
            return;
        }
        Cache cache = cacheManager.getCache(getCacheName());
        if (cache != null) {
            cache.put(getCacheKey(), data);
        }
    }

    /**
     * Retrieves cached data if available, otherwise returns default value.
     *
     * @param cause the exception that triggered fallback
     * @return cached data, default value, or null
     */
    @SuppressWarnings("unchecked")
    protected T getFallbackData(Exception cause) {
        log.warn("Using fallback for {} due to: {}", getSourceName(), cause.getMessage());

        // Try to get from cache
        Cache cache = cacheManager.getCache(getCacheName());
        if (cache != null) {
            Cache.ValueWrapper cached = cache.get(getCacheKey());
            if (cached != null && cached.get() != null) {
                this.servingStaleData = true;
                log.info("Returning cached data for {}", getSourceName());
                return (T) cached.get();
            }
        }

        // No cache available, return default
        log.warn("No cached data for {}, returning default value", getSourceName());
        this.servingStaleData = true;
        return getDefaultValue();
    }

    /**
     * Returns the WebClient for subclasses that need direct access.
     *
     * @return the configured WebClient
     */
    protected WebClient getWebClient() {
        return webClient;
    }

    /**
     * Returns the logger for subclasses.
     *
     * @return the logger instance
     */
    protected Logger getLog() {
        return log;
    }
}
