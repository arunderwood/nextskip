package io.nextskip.propagation.internal.admin;

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
 * Provides admin status for propagation module feeds.
 *
 * <p>Exposes status for:
 * <ul>
 *   <li>NOAA SWPC - Solar flux index and sunspot data (5 min refresh)</li>
 *   <li>HamQSL Solar - Solar indices from HamQSL (30 min refresh)</li>
 *   <li>HamQSL Band - Band condition data from HamQSL (15 min refresh)</li>
 * </ul>
 */
@Component
@ConditionalOnBean(Scheduler.class)
public class PropagationAdminProvider implements AdminStatusProvider {

    private static final String MODULE_NAME = "propagation";

    // Task names matching the RecurringTask configurations
    private static final String NOAA_TASK = "noaa-refresh";
    private static final String HAMQSL_SOLAR_TASK = "hamqsl-solar-refresh";
    private static final String HAMQSL_BAND_TASK = "hamqsl-band-refresh";

    // Display names for feeds
    private static final String NOAA_DISPLAY = "NOAA SWPC";
    private static final String HAMQSL_SOLAR_DISPLAY = "HamQSL Solar";
    private static final String HAMQSL_BAND_DISPLAY = "HamQSL Band";

    // Refresh intervals (must match task configurations)
    private static final long NOAA_INTERVAL_SECONDS = Duration.ofMinutes(5).toSeconds();
    private static final long HAMQSL_SOLAR_INTERVAL_SECONDS = Duration.ofMinutes(30).toSeconds();
    private static final long HAMQSL_BAND_INTERVAL_SECONDS = Duration.ofMinutes(15).toSeconds();

    // Map of display names to task names for triggerRefresh
    private static final Map<String, String> FEED_TO_TASK = Map.of(
            NOAA_DISPLAY, NOAA_TASK,
            HAMQSL_SOLAR_DISPLAY, HAMQSL_SOLAR_TASK,
            HAMQSL_BAND_DISPLAY, HAMQSL_BAND_TASK
    );

    private final Scheduler scheduler;

    public PropagationAdminProvider(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public List<FeedStatus> getFeedStatuses() {
        return List.of(
                buildFeedStatus(NOAA_DISPLAY, NOAA_TASK, NOAA_INTERVAL_SECONDS),
                buildFeedStatus(HAMQSL_SOLAR_DISPLAY, HAMQSL_SOLAR_TASK, HAMQSL_SOLAR_INTERVAL_SECONDS),
                buildFeedStatus(HAMQSL_BAND_DISPLAY, HAMQSL_BAND_TASK, HAMQSL_BAND_INTERVAL_SECONDS)
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
            // Reschedule to run immediately
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
        // Find the scheduled execution for this task
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

            // Calculate approximate last refresh time based on next refresh and interval
            if (nextRefreshTime != null && !isCurrentlyRefreshing) {
                lastRefreshTime = nextRefreshTime.minusSeconds(intervalSeconds);
            }

            // Get last failure time if there were failures
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
        // Query db-scheduler for scheduled executions
        // RecurringTasks use "recurring" as the instance id
        return scheduler.getScheduledExecutions()
                .stream()
                .filter(exec -> taskName.equals(exec.getTaskInstance().getTaskName()))
                .map(exec -> (ScheduledExecution<Object>) exec)
                .findFirst();
    }
}
