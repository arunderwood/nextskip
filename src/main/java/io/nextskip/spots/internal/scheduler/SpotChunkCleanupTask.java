package io.nextskip.spots.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import io.nextskip.spots.persistence.repository.SpotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Recurring task that drops old hypertable chunks from the spots table.
 *
 * <p>Uses TimescaleDB's {@code drop_chunks()} which is available under
 * the Apache license, unlike {@code add_retention_policy()} which
 * requires a Timescale Community license.
 *
 * <p>Runs every hour and drops chunks older than 6 hours, matching the
 * spot data TTL used throughout the application.
 */
@Configuration
@ConditionalOnProperty(prefix = "nextskip.spots", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SpotChunkCleanupTask {

    private static final Logger LOG = LoggerFactory.getLogger(SpotChunkCleanupTask.class);
    private static final String TASK_NAME = "spot-chunk-cleanup";
    private static final Duration CLEANUP_INTERVAL = Duration.ofHours(1);
    private static final String RETENTION_INTERVAL = "6 hours";

    /**
     * Creates the recurring task bean for spot chunk cleanup.
     *
     * @param spotRepository the spot repository with drop_chunks support
     * @return the configured recurring task
     */
    @Bean
    public RecurringTask<Void> spotChunkCleanupRecurringTask(SpotRepository spotRepository) {
        return Tasks.recurring(TASK_NAME, FixedDelay.of(CLEANUP_INTERVAL))
                .execute((taskInstance, executionContext) -> {
                    LOG.info("Dropping spots chunks older than {}", RETENTION_INTERVAL);
                    spotRepository.dropOldChunks(RETENTION_INTERVAL);
                    LOG.info("Spot chunk cleanup complete");
                });
    }
}
