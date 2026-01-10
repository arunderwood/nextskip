package io.nextskip.contests.internal.admin;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.ScheduledExecution;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import io.nextskip.common.admin.AdminStatusProvider;
import io.nextskip.common.admin.FeedStatus;
import io.nextskip.common.admin.ScheduledFeedStatus;
import io.nextskip.common.admin.TriggerRefreshResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Provides admin status for contests module feeds.
 *
 * <p>Exposes status for:
 * <ul>
 *   <li>Contest Calendar - WA7BNM contest calendar data (6 hour refresh)</li>
 * </ul>
 */
@Component
@ConditionalOnBean(Scheduler.class)
public class ContestsAdminProvider implements AdminStatusProvider {

    private static final String MODULE_NAME = "contests";

    // Task names matching the RecurringTask configurations
    private static final String CONTEST_TASK = "contest-refresh";

    // Display names for feeds
    private static final String CONTEST_DISPLAY = "Contest Calendar";

    // Refresh intervals (must match task configurations)
    private static final long CONTEST_INTERVAL_SECONDS = Duration.ofHours(6).toSeconds();

    // Map of display names to task names for triggerRefresh
    private static final Map<String, String> FEED_TO_TASK = Map.of(
            CONTEST_DISPLAY, CONTEST_TASK
    );

    private final Scheduler scheduler;

    public ContestsAdminProvider(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public List<FeedStatus> getFeedStatuses() {
        return List.of(
                buildFeedStatus(CONTEST_DISPLAY, CONTEST_TASK, CONTEST_INTERVAL_SECONDS)
        );
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException") // Scheduler can throw various runtime exceptions
    public Optional<TriggerRefreshResult> triggerRefresh(String feedName) {
        String taskName = FEED_TO_TASK.get(feedName);
        if (taskName == null) {
            return Optional.empty();
        }

        try {
            Instant now = Instant.now();
            scheduler.reschedule(
                    TaskInstanceId.of(taskName, "recurring"),
                    now
            );
            return Optional.of(TriggerRefreshResult.success(feedName, now));
        } catch (Exception e) {
            return Optional.of(new TriggerRefreshResult(
                    false,
                    "Failed to trigger refresh: " + e.getMessage(),
                    feedName,
                    null
            ));
        }
    }

    @Override
    public String getModuleName() {
        return MODULE_NAME;
    }

    private ScheduledFeedStatus buildFeedStatus(String displayName, String taskName, long intervalSeconds) {
        Optional<ScheduledExecution<Object>> execution = findScheduledExecution(taskName);

        Instant lastRefreshTime = null;
        Instant nextRefreshTime = null;
        boolean isCurrentlyRefreshing = false;
        int consecutiveFailures = 0;
        Instant lastFailureTime = null;

        if (execution.isPresent()) {
            ScheduledExecution<Object> exec = execution.get();
            nextRefreshTime = exec.getExecutionTime();
            isCurrentlyRefreshing = exec.isPicked();
            consecutiveFailures = exec.getConsecutiveFailures();

            if (nextRefreshTime != null && !isCurrentlyRefreshing) {
                lastRefreshTime = nextRefreshTime.minusSeconds(intervalSeconds);
            }

            if (consecutiveFailures > 0) {
                lastFailureTime = exec.getLastFailure();
            }
        }

        return ScheduledFeedStatus.of(
                displayName,
                lastRefreshTime,
                nextRefreshTime,
                isCurrentlyRefreshing,
                consecutiveFailures,
                lastFailureTime,
                intervalSeconds
        );
    }

    @SuppressWarnings("unchecked")
    private Optional<ScheduledExecution<Object>> findScheduledExecution(String taskName) {
        return scheduler.getScheduledExecutions()
                .stream()
                .filter(exec -> taskName.equals(exec.getTaskInstance().getTaskName()))
                .map(exec -> (ScheduledExecution<Object>) exec)
                .findFirst();
    }
}
