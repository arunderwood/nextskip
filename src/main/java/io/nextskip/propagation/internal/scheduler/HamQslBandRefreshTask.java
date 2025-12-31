package io.nextskip.propagation.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import io.nextskip.propagation.persistence.repository.BandConditionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
 */
@Configuration
public class HamQslBandRefreshTask {

    private static final String TASK_NAME = "hamqsl-band-refresh";
    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(15);

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

    /**
     * Checks if initial data load is needed.
     *
     * @param repository the band condition repository
     * @return true if no recent band condition data exists
     */
    public boolean needsInitialLoad(BandConditionRepository repository) {
        Instant recent = Instant.now().minus(Duration.ofMinutes(30));
        return repository.findByRecordedAtAfterOrderByRecordedAtDesc(recent).isEmpty();
    }
}
