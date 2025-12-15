package io.nextskip.common.client;

/**
 * Common interface for all clients that fetch data from external APIs.
 *
 * <p>This interface establishes a consistent contract for external API clients across
 * all NextSkip modules (propagation, activations, satellites, etc.), enabling:
 * <ul>
 *     <li>Dependency inversion - services depend on abstractions, not concrete classes</li>
 *     <li>Testability - easy to mock external data sources</li>
 *     <li>Consistency - all clients follow the same resilience patterns</li>
 *     <li>Extensibility - new data sources can be added without modifying existing code</li>
 * </ul>
 *
 * <p>Implementations should use Spring's resilience patterns:
 * <ul>
 *     <li>{@code @CircuitBreaker} - prevent cascading failures</li>
 *     <li>{@code @Retry} - handle transient failures</li>
 *     <li>{@code @Cacheable} - reduce API load and improve response time</li>
 * </ul>
 *
 * @param <T> the type of data returned by this client
 */
public interface ExternalDataClient<T> {

    /**
     * Fetches current data from the external API.
     *
     * <p>This method should be annotated with resilience4j annotations:
     * <pre>
     * {@code @CircuitBreaker(name = "clientName", fallbackMethod = "getCachedFallback")}
     * {@code @Retry(name = "clientName")}
     * {@code @Cacheable(value = "cacheName", unless = "#result == null")}
     * </pre>
     *
     * @return the fetched data, or null if unavailable
     * @throws io.nextskip.propagation.internal.ExternalApiException if the API call fails
     */
    T fetch();

    /**
     * Returns the human-readable name of this data source.
     *
     * <p>Used for logging, error messages, and UI attribution.
     *
     * @return the source name (e.g., "NOAA SWPC", "POTA API", "SOTA API")
     */
    String getSourceName();
}
