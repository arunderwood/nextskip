package io.nextskip.propagation.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import io.nextskip.common.scheduler.RefreshTaskCoordinator;
import io.nextskip.propagation.persistence.repository.BandConditionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.time.Duration;
import java.time.Instant;

/**
 * Recurring task configuration for refreshing HamQSL band condition data.
 *
 * <p>Configures the db-scheduler recurring task that delegates to
 * {@link HamQslBandRefreshService} for the actual refresh logic.
 *
 * <p>Task runs every 15 minutes (slightly more frequent than solar data
 * since band conditions can change more quickly).
 *
 * <p>Implements {@link RefreshTaskCoordinator} to enable automatic discovery
 * by {@link io.nextskip.common.scheduler.DataRefreshStartupHandler}.
 */
@Configuration
public class HamQslBandRefreshTask implements RefreshTaskCoordinator {

    private static final String TASK_NAME = "hamqsl-band-refresh";
    private static final String DISPLAY_NAME = "HamQSL Band";
    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(15);
    private static final Duration STALE_THRESHOLD = Duration.ofMinutes(30);

    private final BandConditionRepository repository;
    private RecurringTask<Void> recurringTask;

    /**
     * Creates a new HamQSL band refresh task coordinator.
     *
     * @param repository the band condition repository
     */
    public HamQslBandRefreshTask(BandConditionRepository repository) {
        this.repository = repository;
    }

    /**
     * Injects the recurring task bean after creation.
     *
     * @param recurringTask the created recurring task bean
     */
    @Autowired
    public void setRecurringTask(@Lazy RecurringTask<Void> hamQslBandRecurringTask) {
        this.recurringTask = hamQslBandRecurringTask;
    }

    /**
     * Creates the recurring task bean for HamQSL band condition refresh.
     *
     * @param refreshService the service that handles the transactional refresh logic
     * @return the configured recurring task
     */
    @Bean
    public RecurringTask<Void> hamQslBandRecurringTask(HamQslBandRefreshService refreshService) {
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
        return repository.findByRecordedAtAfterOrderByRecordedAtDesc(recent).isEmpty();
    }

    @Override
    public String getTaskName() {
        return DISPLAY_NAME;
    }

    /**
     * Checks if initial data load is needed.
     *
     * @param repository the band condition repository
     * @return true if no recent band condition data exists
     * @deprecated Use {@link #needsInitialLoad()} instead.
     */
    @Deprecated(forRemoval = true)
    public boolean needsInitialLoad(BandConditionRepository repository) {
        Instant recent = Instant.now().minus(STALE_THRESHOLD);
        return repository.findByRecordedAtAfterOrderByRecordedAtDesc(recent).isEmpty();
    }
}
