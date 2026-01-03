package io.nextskip.common.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 *   <li>Freshness Tracking - tracks data age for UI display</li>
 * </ul>
 *
 * <p>Note: Clients do not cache data. Database persistence provides the fallback
 * mechanism. When a fetch fails, exceptions propagate to the caller (typically
 * a refresh service), and services continue serving data from the database-backed
 * LoadingCache.
 *
 * <p>Uses programmatic Resilience4j APIs (not annotations) to allow dynamic
 * circuit breaker names via {@link #getClientName()}.
 *
 * <p>Subclasses implement:
 * <ul>
 *   <li>{@link #getClientName()} - identifier for circuit breaker/retry instances</li>
 *   <li>{@link #doFetch()} - pure API fetch logic</li>
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

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    // Freshness tracking
    private volatile Instant lastSuccessfulRefresh;

    /**
     * Constructs an AbstractExternalDataClient with the given dependencies.
     *
     * @param webClientBuilder builder for creating WebClient
     * @param circuitBreakerRegistry registry for circuit breakers
     * @param retryRegistry registry for retry configurations
     * @param baseUrl the base URL for the external API
     */
    protected AbstractExternalDataClient(
            WebClient.Builder webClientBuilder,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            String baseUrl) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(getMaxResponseSize()))
                .build();
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
    public final T fetch() {
        Supplier<T> decoratedFetch = () -> {
            try {
                T result = doFetch();

                // Update freshness tracking
                this.lastSuccessfulRefresh = Instant.now();

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
        // Exceptions propagate to caller - no fallback at client level
        // Fallback is handled via database-backed LoadingCache at service level
        Supplier<T> retryWrapped = Retry.decorateSupplier(retry, decoratedFetch);
        Supplier<T> cbWrapped = CircuitBreaker.decorateSupplier(circuitBreaker, retryWrapped);

        return cbWrapped.get();
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
