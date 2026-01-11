package io.nextskip.spots.internal.admin;

import com.github.kagkarlsson.scheduler.Scheduler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nextskip.common.admin.AbstractScheduledAdminProvider;
import io.nextskip.common.admin.ConnectionState;
import io.nextskip.common.admin.FeedStatus;
import io.nextskip.common.admin.ScheduledFeedDefinition;
import io.nextskip.common.admin.SubscriptionFeedStatus;
import io.nextskip.common.admin.TriggerRefreshResult;
import io.nextskip.spots.internal.client.SpotSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Provides admin status for spots module feeds.
 *
 * <p>Exposes status for:
 * <ul>
 *   <li>PSKReporter MQTT - Real-time spot subscription (subscription feed)</li>
 *   <li>Band Activity - Aggregated band activity data (1 min scheduled refresh)</li>
 * </ul>
 *
 * <p>This provider has mixed feed types, overriding base class methods to handle
 * both scheduled and subscription feeds.
 */
@Component
@ConditionalOnBean(Scheduler.class)
@ConditionalOnProperty(prefix = "nextskip.spots", name = "enabled", havingValue = "true", matchIfMissing = true)
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-injected beans are managed by Spring container")
public class SpotsAdminProvider extends AbstractScheduledAdminProvider {

    private static final String MODULE_NAME = "spots";
    private static final String PSKREPORTER_DISPLAY = "PSKReporter MQTT";

    private static final List<ScheduledFeedDefinition> SCHEDULED_FEEDS = List.of(
            new ScheduledFeedDefinition(
                    "Band Activity",
                    "band-activity-refresh",
                    Duration.ofMinutes(1).toSeconds()
            )
    );

    private final SpotSource spotSource;

    public SpotsAdminProvider(Scheduler scheduler, SpotSource spotSource) {
        super(scheduler, MODULE_NAME, SCHEDULED_FEEDS);
        this.spotSource = spotSource;
    }

    @Override
    public List<FeedStatus> getFeedStatuses() {
        List<FeedStatus> statuses = new ArrayList<>();

        // Subscription feed: PSKReporter MQTT
        statuses.add(buildSubscriptionFeedStatus());

        // Scheduled feeds from base class
        statuses.addAll(super.getFeedStatuses());

        return statuses;
    }

    @Override
    protected Optional<TriggerRefreshResult> handleNonScheduledFeed(String feedName) {
        if (PSKREPORTER_DISPLAY.equals(feedName)) {
            return Optional.of(TriggerRefreshResult.notScheduledFeed(feedName));
        }
        return Optional.empty();
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

        return SubscriptionFeedStatus.of(
                PSKREPORTER_DISPLAY,
                connectionState,
                null, // lastMessageTime not exposed from SpotSource
                0     // consecutiveReconnectAttempts not exposed from SpotSource
        );
    }
}
