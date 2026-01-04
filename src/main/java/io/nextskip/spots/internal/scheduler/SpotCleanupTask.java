package io.nextskip.spots.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Recurring task configuration for cleaning up expired spots.
 *
 * <p>Configures the db-scheduler recurring task that delegates to
 * {@link SpotCleanupService} for the actual cleanup logic.
 *
 * <p>Task runs at a configurable interval (default: 1 hour).
 *
 * <p>Unlike refresh tasks, this task does not implement {@code RefreshTaskCoordinator}
 * because it doesn't need initial loading or cache management.
 */
@Configuration
@ConditionalOnProperty(prefix = "nextskip.spots", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SpotCleanupTask {

    private static final String TASK_NAME = "spot-cleanup";

    /**
     * Creates the recurring task bean for spot cleanup.
     *
     * @param cleanupService the service that handles the transactional cleanup logic
     * @param cleanupInterval how often to run cleanup
     * @return the configured recurring task
     */
    @Bean
    public RecurringTask<Void> spotCleanupRecurringTask(
            SpotCleanupService cleanupService,
            @Value("${nextskip.spots.persistence.cleanup-interval:1h}") Duration cleanupInterval) {
        return Tasks.recurring(TASK_NAME, FixedDelay.of(cleanupInterval))
                .execute((taskInstance, executionContext) -> cleanupService.executeCleanup());
    }
}
