package io.nextskip.activations.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.activations.persistence.repository.ActivationRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.Instant;

/**
 * Recurring task configuration for refreshing SOTA activation data.
 *
 * <p>Configures the db-scheduler recurring task that delegates to
 * {@link SotaRefreshService} for the actual refresh logic.
 *
 * <p>Task runs every 1 minute (matching the original refresh interval).
 */
@Configuration
public class SotaRefreshTask {

    private static final String TASK_NAME = "sota-refresh";
    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(1);

    /**
     * Creates the recurring task bean for SOTA data refresh.
     *
     * @param refreshService the service that handles the transactional refresh logic
     * @return the configured recurring task
     */
    @Bean
    public RecurringTask<Void> sotaRecurringTask(SotaRefreshService refreshService) {
        return Tasks.recurring(TASK_NAME, FixedDelay.of(REFRESH_INTERVAL))
                .execute((taskInstance, executionContext) -> refreshService.executeRefresh());
    }

    /**
     * Checks if initial data load is needed.
     *
     * @param repository the activation repository
     * @return true if no recent SOTA data exists
     */
    public boolean needsInitialLoad(ActivationRepository repository) {
        Instant recent = Instant.now().minus(Duration.ofMinutes(5));
        return repository.findByTypeAndSpottedAtAfterOrderBySpottedAtDesc(
                ActivationType.SOTA, recent).isEmpty();
    }
}
