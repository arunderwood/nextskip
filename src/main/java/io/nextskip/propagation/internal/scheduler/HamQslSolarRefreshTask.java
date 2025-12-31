package io.nextskip.propagation.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import io.nextskip.propagation.persistence.repository.SolarIndicesRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.Instant;

/**
 * Recurring task configuration for refreshing HamQSL solar indices data.
 *
 * <p>Configures the db-scheduler recurring task that delegates to
 * {@link HamQslSolarRefreshService} for the actual refresh logic.
 *
 * <p>Task runs every 30 minutes (matching the HamQSL client refresh interval).
 */
@Configuration
public class HamQslSolarRefreshTask {

    private static final String TASK_NAME = "hamqsl-solar-refresh";
    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(30);
    private static final String HAMQSL_SOURCE = "HamQSL";

    /**
     * Creates the recurring task bean for HamQSL solar data refresh.
     *
     * @param refreshService the service that handles the transactional refresh logic
     * @return the configured recurring task
     */
    @Bean
    public RecurringTask<Void> hamQslSolarRecurringTask(HamQslSolarRefreshService refreshService) {
        return Tasks.recurring(TASK_NAME, FixedDelay.of(REFRESH_INTERVAL))
                .execute((taskInstance, executionContext) -> refreshService.executeRefresh());
    }

    /**
     * Checks if initial data load is needed.
     *
     * @param repository the solar indices repository
     * @return true if no recent HamQSL data exists
     */
    public boolean needsInitialLoad(SolarIndicesRepository repository) {
        Instant recent = Instant.now().minus(Duration.ofMinutes(60));
        return repository.findByTimestampAfterOrderByTimestampDesc(recent).stream()
                .noneMatch(e -> HAMQSL_SOURCE.equals(e.getSource()));
    }
}
