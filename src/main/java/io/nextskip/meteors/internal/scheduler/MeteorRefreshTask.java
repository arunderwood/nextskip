package io.nextskip.meteors.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import io.nextskip.common.scheduler.RefreshTaskCoordinator;
import io.nextskip.meteors.persistence.repository.MeteorShowerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

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
 *
 * <p>Implements {@link RefreshTaskCoordinator} to enable automatic discovery
 * by {@link io.nextskip.common.scheduler.DataRefreshStartupHandler}.
 */
@Configuration
public class MeteorRefreshTask implements RefreshTaskCoordinator {

    private static final String TASK_NAME = "meteor-refresh";
    private static final String DISPLAY_NAME = "Meteor";
    private static final Duration REFRESH_INTERVAL = Duration.ofHours(1);

    private final MeteorShowerRepository repository;
    private RecurringTask<Void> recurringTask;

    /**
     * Creates a new meteor refresh task coordinator.
     *
     * @param repository the meteor shower repository
     */
    public MeteorRefreshTask(MeteorShowerRepository repository) {
        this.repository = repository;
    }

    /**
     * Injects the recurring task bean after creation.
     *
     * @param recurringTask the created recurring task bean
     */
    @Autowired
    public void setRecurringTask(@Lazy RecurringTask<Void> meteorRecurringTask) {
        this.recurringTask = meteorRecurringTask;
    }

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

    @Override
    public RecurringTask<Void> getRecurringTask() {
        return recurringTask;
    }

    @Override
    public boolean needsInitialLoad() {
        // Check if we have any showers with visibility ending in the future
        Instant now = Instant.now();
        return repository.findByVisibilityStartBeforeAndVisibilityEndAfterOrderByPeakStartAsc(
                now, now).isEmpty()
                && repository.findByVisibilityStartAfterOrderByVisibilityStartAsc(now).isEmpty();
    }

    @Override
    public String getTaskName() {
        return DISPLAY_NAME;
    }

    /**
     * Checks if initial data load is needed.
     *
     * @param repository the meteor shower repository
     * @return true if no meteor shower data exists
     * @deprecated Use {@link #needsInitialLoad()} instead.
     */
    @Deprecated(forRemoval = true)
    public boolean needsInitialLoad(MeteorShowerRepository repository) {
        Instant now = Instant.now();
        return repository.findByVisibilityStartBeforeAndVisibilityEndAfterOrderByPeakStartAsc(
                now, now).isEmpty()
                && repository.findByVisibilityStartAfterOrderByVisibilityStartAsc(now).isEmpty();
    }
}
