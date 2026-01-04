package io.nextskip.spots.internal.stream;

import org.apache.pekko.actor.ActorSystem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuration for the Pekko ActorSystem used by spot stream processing.
 *
 * <p>Creates a module-scoped ActorSystem that provides:
 * <ul>
 *   <li>Isolated lifecycle from other modules</li>
 *   <li>Clean shutdown via destroyMethod</li>
 *   <li>Dedicated dispatcher for spot processing</li>
 * </ul>
 *
 * <p>The ActorSystem is only created when spots processing is enabled.
 */
@Configuration
@ConditionalOnProperty(prefix = "nextskip.spots", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SpotStreamConfig {

    /**
     * Creates the ActorSystem for spot stream processing.
     *
     * <p>The system is named "spots" to distinguish it in logs and metrics.
     * The destroyMethod ensures clean shutdown when the application stops.
     *
     * @return the ActorSystem for spot processing
     */
    @Bean(destroyMethod = "terminate")
    public ActorSystem spotActorSystem() {
        return ActorSystem.create("spots");
    }

    /**
     * Creates a dedicated thread pool for database persistence operations.
     *
     * <p>Using a dedicated executor prevents blocking I/O from starving
     * {@link java.util.concurrent.ForkJoinPool#commonPool()}. The thread count
     * is configurable to tune for single-core vs multi-core environments.
     *
     * <p>Default is 2 threads, optimized for single-core production where
     * DB operations are I/O-bound (allows overlap while waiting for I/O).
     *
     * @param threads number of threads in the pool (default: 2)
     * @return the ExecutorService for spot persistence
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService spotPersistenceExecutor(
            @Value("${nextskip.spots.processing.persistence-threads:2}") int threads) {
        return Executors.newFixedThreadPool(threads, runnable -> {
            Thread thread = new Thread(runnable, "spot-persist");
            thread.setDaemon(true);
            return thread;
        });
    }
}
