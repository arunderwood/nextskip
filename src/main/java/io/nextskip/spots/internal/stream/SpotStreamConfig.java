package io.nextskip.spots.internal.stream;

import org.apache.pekko.actor.ActorSystem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
