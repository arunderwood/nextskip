package io.nextskip.activations.internal.admin;

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
 * Provides admin status for activations module feeds.
 *
 * <p>Exposes status for:
 * <ul>
 *   <li>POTA - Parks on the Air activation spots (1 min refresh)</li>
 *   <li>SOTA - Summits on the Air activation spots (1 min refresh)</li>
 * </ul>
 */
@Component
@ConditionalOnBean(Scheduler.class)
public class ActivationsAdminProvider implements AdminStatusProvider {

    private static final String MODULE_NAME = "activations";

    // Task names matching the RecurringTask configurations
    private static final String POTA_TASK = "pota-refresh";
    private static final String SOTA_TASK = "sota-refresh";

    // Display names for feeds
    private static final String POTA_DISPLAY = "POTA";
    private static final String SOTA_DISPLAY = "SOTA";

    // Refresh intervals (must match task configurations)
    private static final long POTA_INTERVAL_SECONDS = Duration.ofMinutes(1).toSeconds();
    private static final long SOTA_INTERVAL_SECONDS = Duration.ofMinutes(1).toSeconds();

    // Map of display names to task names for triggerRefresh
    private static final Map<String, String> FEED_TO_TASK = Map.of(
            POTA_DISPLAY, POTA_TASK,
            SOTA_DISPLAY, SOTA_TASK
    );

    private final Scheduler scheduler;

    public ActivationsAdminProvider(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public List<FeedStatus> getFeedStatuses() {
        return List.of(
                buildFeedStatus(POTA_DISPLAY, POTA_TASK, POTA_INTERVAL_SECONDS),
                buildFeedStatus(SOTA_DISPLAY, SOTA_TASK, SOTA_INTERVAL_SECONDS)
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
