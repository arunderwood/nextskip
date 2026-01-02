package io.nextskip.propagation.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import io.nextskip.common.scheduler.RefreshTaskCoordinator;
import io.nextskip.propagation.persistence.repository.SolarIndicesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.time.Duration;
import java.time.Instant;

/**
 * Recurring task configuration for refreshing NOAA SWPC solar indices data.
 *
 * <p>Configures the db-scheduler recurring task that delegates to
 * {@link NoaaRefreshService} for the actual refresh logic.
 *
 * <p>Task runs every 5 minutes (matching the NOAA client refresh interval).
 *
 * <p>Implements {@link RefreshTaskCoordinator} to enable automatic discovery
 * by {@link io.nextskip.common.scheduler.DataRefreshStartupHandler}.
 */
@Configuration
public class NoaaRefreshTask implements RefreshTaskCoordinator {

    private static final String TASK_NAME = "noaa-refresh";
    private static final String DISPLAY_NAME = "NOAA";
    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(5);
    private static final String NOAA_SOURCE = "NOAA SWPC";
    private static final Duration STALE_THRESHOLD = Duration.ofMinutes(10);

    private final SolarIndicesRepository repository;
    private RecurringTask<Void> recurringTask;

    /**
     * Creates a new NOAA refresh task coordinator.
     *
     * @param repository the solar indices repository
     */
    public NoaaRefreshTask(SolarIndicesRepository repository) {
        this.repository = repository;
    }

    /**
     * Injects the recurring task bean after creation.
     *
     * <p>Uses {@code @Lazy} to break the circular dependency between
     * this configuration class and the bean it creates.
     *
     * @param recurringTask the created recurring task bean
     */
    @Autowired
    public void setRecurringTask(@Lazy RecurringTask<Void> noaaRecurringTask) {
        this.recurringTask = noaaRecurringTask;
    }

    /**
     * Creates the recurring task bean for NOAA data refresh.
     *
     * @param refreshService the service that handles the transactional refresh logic
     * @return the configured recurring task
     */
    @Bean
    public RecurringTask<Void> noaaRecurringTask(NoaaRefreshService refreshService) {
        return Tasks.recurring(TASK_NAME, FixedDelay.of(REFRESH_INTERVAL))
                .execute((taskInstance, executionContext) -> refreshService.executeRefresh());
    }

    @Override
    public RecurringTask<Void> getRecurringTask() {
        return recurringTask;
    }

    @Override
    public boolean needsInitialLoad() {
        Instant recent = Instant.now().minus(STALE_THRESHOLD);
        return repository.findByTimestampAfterOrderByTimestampDesc(recent).stream()
                .noneMatch(e -> NOAA_SOURCE.equals(e.getSource()));
    }

    @Override
    public String getTaskName() {
        return DISPLAY_NAME;
    }

    /**
     * Checks if initial data load is needed.
     *
     * @param repository the solar indices repository
     * @return true if no recent NOAA data exists
     * @deprecated Use {@link #needsInitialLoad()} instead. This method is kept
     *             for backward compatibility during migration.
     */
    @Deprecated(forRemoval = true)
    public boolean needsInitialLoad(SolarIndicesRepository repository) {
        Instant recent = Instant.now().minus(STALE_THRESHOLD);
        return repository.findByTimestampAfterOrderByTimestampDesc(recent).stream()
                .noneMatch(e -> NOAA_SOURCE.equals(e.getSource()));
    }
}
