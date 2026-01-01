package io.nextskip.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Configuration for time-based operations.
 *
 * <p>Provides a default system clock that can be overridden in tests
 * for deterministic time-based assertions.
 *
 * <p>Usage in tests:
 * <pre>{@code
 * private static final Instant FIXED_TIME = Instant.parse("2025-01-15T12:00:00Z");
 * private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_TIME, ZoneOffset.UTC);
 *
 * @BeforeEach
 * void setUp() {
 *     service = new SomeService(dependency, FIXED_CLOCK);
 * }
 * }</pre>
 */
@Configuration
public class ClockConfig {

    /**
     * Provides a default system clock using UTC timezone.
     *
     * <p>This bean can be overridden in tests by defining a {@code Clock.fixed()}
     * bean, which will take precedence due to {@code @ConditionalOnMissingBean}.
     *
     * @return the system clock in UTC
     */
    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
