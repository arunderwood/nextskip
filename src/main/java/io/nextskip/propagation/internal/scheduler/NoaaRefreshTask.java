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
 * Recurring task configuration for refreshing NOAA SWPC solar indices data.
 *
 * <p>Configures the db-scheduler recurring task that delegates to
 * {@link NoaaRefreshService} for the actual refresh logic.
 *
 * <p>Task runs every 5 minutes (matching the NOAA client refresh interval).
 */
@Configuration
public class NoaaRefreshTask {

    private static final String TASK_NAME = "noaa-refresh";
    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(5);
    private static final String NOAA_SOURCE = "NOAA SWPC";

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

    /**
     * Checks if initial data load is needed.
     *
     * @param repository the solar indices repository
     * @return true if no recent NOAA data exists
     */
    public boolean needsInitialLoad(SolarIndicesRepository repository) {
        Instant recent = Instant.now().minus(Duration.ofMinutes(10));
        return repository.findByTimestampAfterOrderByTimestampDesc(recent).stream()
                .noneMatch(e -> NOAA_SOURCE.equals(e.getSource()));
    }
}
