package io.nextskip.contests.internal.admin;

import com.github.kagkarlsson.scheduler.Scheduler;
import io.nextskip.common.admin.AbstractScheduledAdminProvider;
import io.nextskip.common.admin.ScheduledFeedDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

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
public class ContestsAdminProvider extends AbstractScheduledAdminProvider {

    private static final String MODULE_NAME = "contests";

    private static final List<ScheduledFeedDefinition> FEEDS = List.of(
            new ScheduledFeedDefinition(
                    "Contest Calendar",
                    "contest-refresh",
                    Duration.ofHours(6).toSeconds()
            )
    );

    public ContestsAdminProvider(Scheduler scheduler) {
        super(scheduler, MODULE_NAME, FEEDS);
    }
}
