package io.nextskip.meteors.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import io.nextskip.meteors.persistence.repository.MeteorShowerRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.Instant;

/**
 * Recurring task configuration for refreshing meteor shower data.
 *
 * <p>Configures the db-scheduler recurring task that delegates to
 * {@link MeteorRefreshService} for the actual refresh logic.
 *
 * <p>Task runs every 1 hour (shower dates are static, but allows for
 * year-end transitions and data file updates).
 */
@Configuration
public class MeteorRefreshTask {

    private static final String TASK_NAME = "meteor-refresh";
    private static final Duration REFRESH_INTERVAL = Duration.ofHours(1);

    /**
     * Creates the recurring task bean for meteor shower data refresh.
     *
     * @param refreshService the service that handles the transactional refresh logic
     * @return the configured recurring task
     */
    @Bean
    public RecurringTask<Void> meteorRecurringTask(MeteorRefreshService refreshService) {
        return Tasks.recurring(TASK_NAME, FixedDelay.of(REFRESH_INTERVAL))
                .execute((taskInstance, executionContext) -> refreshService.executeRefresh());
    }

    /**
     * Checks if initial data load is needed.
     *
     * @param repository the meteor shower repository
     * @return true if no meteor shower data exists
     */
    public boolean needsInitialLoad(MeteorShowerRepository repository) {
        // Check if we have any showers with visibility ending in the future
        Instant now = Instant.now();
        return repository.findByVisibilityStartBeforeAndVisibilityEndAfterOrderByPeakStartAsc(
                now, now).isEmpty()
                && repository.findByVisibilityStartAfterOrderByVisibilityStartAsc(now).isEmpty();
    }
}
