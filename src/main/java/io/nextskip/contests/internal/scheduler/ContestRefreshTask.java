package io.nextskip.contests.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import io.nextskip.contests.persistence.repository.ContestRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.Instant;

/**
 * Recurring task configuration for refreshing contest calendar data.
 *
 * <p>Configures the db-scheduler recurring task that delegates to
 * {@link ContestRefreshService} for the actual refresh logic.
 *
 * <p>Task runs every 6 hours (contest schedules change infrequently).
 */
@Configuration
public class ContestRefreshTask {

    private static final String TASK_NAME = "contest-refresh";
    private static final Duration REFRESH_INTERVAL = Duration.ofHours(6);

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

    /**
     * Checks if initial data load is needed.
     *
     * @param repository the contest repository
     * @return true if no recent contest data exists
     */
    public boolean needsInitialLoad(ContestRepository repository) {
        // Check if we have any contests ending after now
        Instant now = Instant.now();
        return repository.findByEndTimeAfterOrderByStartTimeAsc(now).isEmpty();
    }
}
