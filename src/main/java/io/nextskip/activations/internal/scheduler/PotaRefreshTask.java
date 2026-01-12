package io.nextskip.activations.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.activations.persistence.repository.ActivationRepository;
import io.nextskip.common.scheduler.RefreshTaskCoordinator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.time.Duration;
import java.time.Instant;

/**
 * Recurring task configuration for refreshing POTA activation data.
 *
 * <p>Configures the db-scheduler recurring task that delegates to
 * {@link PotaRefreshService} for the actual refresh logic.
 *
 * <p>Task runs every 1 minute (matching the original refresh interval).
 *
 * <p>Implements {@link RefreshTaskCoordinator} to enable automatic discovery
 * by {@link io.nextskip.common.scheduler.DataRefreshStartupHandler}.
 */
@Configuration
public class PotaRefreshTask implements RefreshTaskCoordinator {

    private static final String TASK_NAME = "pota-refresh";
    private static final String DISPLAY_NAME = "POTA";
    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(1);
    private static final Duration STALE_THRESHOLD = Duration.ofMinutes(5);

    private final ActivationRepository repository;
    private RecurringTask<Void> recurringTask;

    /**
     * Creates a new POTA refresh task coordinator.
     *
     * @param repository the activation repository
     */
    public PotaRefreshTask(ActivationRepository repository) {
        this.repository = repository;
    }

    /**
     * Injects the recurring task bean after creation.
     *
     * @param recurringTask the created recurring task bean
     */
    @Autowired
    public void setRecurringTask(
            @Lazy @Qualifier("potaRecurringTask") RecurringTask<Void> task) {
        this.recurringTask = task;
    }

    /**
     * Creates the recurring task bean for POTA data refresh.
     *
     * @param refreshService the service that handles the transactional refresh logic
     * @return the configured recurring task
     */
    @Bean
    public RecurringTask<Void> potaRecurringTask(PotaRefreshService refreshService) {
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
        return repository.findByTypeAndSpottedAtAfterOrderBySpottedAtDesc(
                ActivationType.POTA, recent).isEmpty();
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
     * @param repository the activation repository
     * @return true if no recent POTA data exists
     * @deprecated Use {@link #needsInitialLoad()} instead.
     */
    @Deprecated(forRemoval = true)
    public boolean needsInitialLoad(ActivationRepository repository) {
        Instant recent = Instant.now().minus(STALE_THRESHOLD);
        return repository.findByTypeAndSpottedAtAfterOrderBySpottedAtDesc(
                ActivationType.POTA, recent).isEmpty();
    }
}
