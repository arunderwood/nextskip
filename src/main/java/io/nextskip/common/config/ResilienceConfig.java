package io.nextskip.common.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j configuration for circuit breakers and retry logic.
 *
 * Provides default configurations for:
 * - Circuit breakers (prevent cascading failures)
 * - Retry logic (transient failure recovery)
 *
 * These will be applied to external API calls to ensure
 * graceful degradation when services are unavailable.
 */
@Configuration
public class ResilienceConfig {

    /**
     * Default circuit breaker configuration.
     *
     * Opens circuit after 50% failure rate in sliding window.
     * Allows limited requests in half-open state to test recovery.
     */
    @Bean
    public CircuitBreakerConfig defaultCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
    }

    /**
     * Default retry configuration.
     *
     * Retries up to 3 times with exponential backoff.
     */
    @Bean
    public RetryConfig defaultRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(java.io.IOException.class,
                        org.springframework.web.reactive.function.client.WebClientRequestException.class)
                .build();
    }
}
