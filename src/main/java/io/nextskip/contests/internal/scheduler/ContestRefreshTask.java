package io.nextskip.contests.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import io.nextskip.common.scheduler.RefreshTaskCoordinator;
import io.nextskip.contests.persistence.repository.ContestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.time.Duration;
import java.time.Instant;

/**
 * Recurring task configuration for refreshing contest calendar data.
 *
 * <p>Configures the db-scheduler recurring task that delegates to
 * {@link ContestRefreshService} for the actual refresh logic.
 *
 * <p>Task runs every 12 hours. Contest schedules are typically published
 * weekly, so 12-hour polling balances freshness with bandwidth efficiency.
 *
 * <p>Implements {@link RefreshTaskCoordinator} to enable automatic discovery
 * by {@link io.nextskip.common.scheduler.DataRefreshStartupHandler}.
 */
@Configuration
public class ContestRefreshTask implements RefreshTaskCoordinator {

    private static final String TASK_NAME = "contest-refresh";
    private static final String DISPLAY_NAME = "Contest";
    private static final Duration REFRESH_INTERVAL = Duration.ofHours(12);

    private final ContestRepository repository;
    private RecurringTask<Void> recurringTask;

    /**
     * Creates a new contest refresh task coordinator.
     *
     * @param repository the contest repository
     */
    public ContestRefreshTask(ContestRepository repository) {
        this.repository = repository;
    }

    /**
     * Injects the recurring task bean after creation.
     *
     * @param recurringTask the created recurring task bean
     */
    @Autowired
    public void setRecurringTask(
            @Lazy @Qualifier("contestRecurringTask") RecurringTask<Void> task) {
        this.recurringTask = task;
    }

    /**
     * Creates the recurring task bean for contest data refresh.
     *
     * @param refreshService the service that handles the transactional refresh logic
     * @return the configured recurring task
     */
    @Bean
    public RecurringTask<Void> contestRecurringTask(ContestRefreshService refreshService) {
        return Tasks.recurring(TASK_NAME, FixedDelay.of(REFRESH_INTERVAL))
                .execute((taskInstance, executionContext) -> refreshService.executeRefresh());
    }

    @Override
    public RecurringTask<Void> getRecurringTask() {
        return recurringTask;
    }

    @Override
    public boolean needsInitialLoad() {
        // Check if we have any contests ending after now
        Instant now = Instant.now();
        return repository.findByEndTimeAfterOrderByStartTimeAsc(now).isEmpty();
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    /**
     * Checks if initial data load is needed.
     *
     * @param repository the contest repository
     * @return true if no recent contest data exists
     * @deprecated Use {@link #needsInitialLoad()} instead.
     */
    @Deprecated(forRemoval = true)
    public boolean needsInitialLoad(ContestRepository repository) {
        Instant now = Instant.now();
        return repository.findByEndTimeAfterOrderByStartTimeAsc(now).isEmpty();
    }
}
