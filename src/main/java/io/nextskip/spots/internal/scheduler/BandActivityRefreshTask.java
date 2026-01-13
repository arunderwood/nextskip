package io.nextskip.spots.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import io.nextskip.common.scheduler.RefreshTaskCoordinator;
import io.nextskip.spots.persistence.repository.SpotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.time.Duration;
import java.time.Instant;

/**
 * Recurring task configuration for refreshing band activity aggregations.
 *
 * <p>Configures the db-scheduler recurring task that delegates to
 * {@link BandActivityRefreshService} for the actual aggregation logic.
 *
 * <p>Task runs every 1 minute to keep band activity data fresh for the dashboard.
 * This is more frequent than other refresh tasks because band activity changes
 * rapidly based on real-time spot data.
 *
 * <p>Implements {@link RefreshTaskCoordinator} to enable automatic discovery
 * by {@link io.nextskip.common.scheduler.DataRefreshStartupHandler}.
 */
@Configuration
@ConditionalOnProperty(prefix = "nextskip.spots", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BandActivityRefreshTask implements RefreshTaskCoordinator {

    private static final String TASK_NAME = "band-activity-refresh";
    private static final String DISPLAY_NAME = "Band Activity";
    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(1);
    private static final Duration STALE_THRESHOLD = Duration.ofMinutes(5);

    private final SpotRepository spotRepository;
    private RecurringTask<Void> recurringTask;

    /**
     * Creates a new band activity refresh task coordinator.
     *
     * @param spotRepository the spot repository
     */
    public BandActivityRefreshTask(SpotRepository spotRepository) {
        this.spotRepository = spotRepository;
    }

    /**
     * Injects the recurring task bean after creation.
     *
     * @param task the created recurring task bean
     */
    @Autowired
    public void setRecurringTask(
            @Lazy @Qualifier("bandActivityRecurringTask") RecurringTask<Void> task) {
        this.recurringTask = task;
    }

    /**
     * Creates the recurring task bean for band activity aggregation.
     *
     * @param refreshService the service that handles the aggregation logic
     * @return the configured recurring task
     */
    @Bean
    public RecurringTask<Void> bandActivityRecurringTask(BandActivityRefreshService refreshService) {
        return Tasks.recurring(TASK_NAME, FixedDelay.of(REFRESH_INTERVAL))
                .execute((taskInstance, executionContext) -> refreshService.executeRefresh());
    }

    @Override
    public RecurringTask<Void> getRecurringTask() {
        return recurringTask;
    }

    /**
     * Checks if initial aggregation is needed.
     *
     * <p>Returns true if there are recent spots to aggregate (within the last 5 minutes).
     * This ensures we don't trigger aggregation on startup when there's no data.
     *
     * @return true if there are recent spots to aggregate
     */
    @Override
    public boolean needsInitialLoad() {
        Instant recent = Instant.now().minus(STALE_THRESHOLD);
        return spotRepository.countByCreatedAtAfter(recent) > 0;
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }
}
