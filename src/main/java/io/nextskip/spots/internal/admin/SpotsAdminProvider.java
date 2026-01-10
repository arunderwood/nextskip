package io.nextskip.spots.internal.admin;

import com.github.kagkarlsson.scheduler.Scheduler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.github.kagkarlsson.scheduler.ScheduledExecution;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import io.nextskip.common.admin.AdminStatusProvider;
import io.nextskip.common.admin.ConnectionState;
import io.nextskip.common.admin.FeedStatus;
import io.nextskip.common.admin.ScheduledFeedStatus;
import io.nextskip.common.admin.SubscriptionFeedStatus;
import io.nextskip.common.admin.TriggerRefreshResult;
import io.nextskip.spots.internal.client.SpotSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Provides admin status for spots module feeds.
 *
 * <p>Exposes status for:
 * <ul>
 *   <li>PSKReporter MQTT - Real-time spot subscription (subscription feed)</li>
 *   <li>Band Activity - Aggregated band activity data (1 min scheduled refresh)</li>
 * </ul>
 */
@Component
@ConditionalOnBean(Scheduler.class)
@ConditionalOnProperty(prefix = "nextskip.spots", name = "enabled", havingValue = "true", matchIfMissing = true)
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-injected beans are managed by Spring container")
public class SpotsAdminProvider implements AdminStatusProvider {

    private static final String MODULE_NAME = "spots";

    // Task names matching the RecurringTask configurations
    private static final String BAND_ACTIVITY_TASK = "band-activity-refresh";

    // Display names for feeds
    private static final String PSKREPORTER_DISPLAY = "PSKReporter MQTT";
    private static final String BAND_ACTIVITY_DISPLAY = "Band Activity";

    // Refresh intervals (must match task configurations)
    private static final long BAND_ACTIVITY_INTERVAL_SECONDS = Duration.ofMinutes(1).toSeconds();

    // Map of display names to task names for triggerRefresh (only scheduled feeds)
    private static final Map<String, String> FEED_TO_TASK = Map.of(
            BAND_ACTIVITY_DISPLAY, BAND_ACTIVITY_TASK
    );

    private final Scheduler scheduler;
    private final SpotSource spotSource;

    public SpotsAdminProvider(Scheduler scheduler, SpotSource spotSource) {
        this.scheduler = scheduler;
        this.spotSource = spotSource;
    }

    @Override
    public List<FeedStatus> getFeedStatuses() {
        List<FeedStatus> statuses = new ArrayList<>();

        // Subscription feed: PSKReporter MQTT
        statuses.add(buildSubscriptionFeedStatus());

        // Scheduled feed: Band Activity aggregation
        statuses.add(buildScheduledFeedStatus(
                BAND_ACTIVITY_DISPLAY,
                BAND_ACTIVITY_TASK,
                BAND_ACTIVITY_INTERVAL_SECONDS
        ));

        return statuses;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException") // Scheduler can throw various runtime exceptions
    public Optional<TriggerRefreshResult> triggerRefresh(String feedName) {
        // Check if it's the subscription feed
        if (PSKREPORTER_DISPLAY.equals(feedName)) {
            return Optional.of(TriggerRefreshResult.notScheduledFeed(feedName));
        }

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

    private SubscriptionFeedStatus buildSubscriptionFeedStatus() {
        boolean isConnected = spotSource.isConnected();
        boolean isReceivingMessages = spotSource.isReceivingMessages();

        ConnectionState connectionState;
        if (isConnected && isReceivingMessages) {
            connectionState = ConnectionState.CONNECTED;
        } else if (isConnected && !isReceivingMessages) {
            connectionState = ConnectionState.STALE;
        } else {
            connectionState = ConnectionState.DISCONNECTED;
        }

        // Note: We don't track last message time or reconnect attempts in admin UI
        // The SpotSource handles this internally
        return SubscriptionFeedStatus.of(
                PSKREPORTER_DISPLAY,
                connectionState,
                null, // lastMessageTime not exposed from SpotSource
                0     // consecutiveReconnectAttempts not exposed from SpotSource
        );
    }

    private ScheduledFeedStatus buildScheduledFeedStatus(String displayName, String taskName, long intervalSeconds) {
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
