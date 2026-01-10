package io.nextskip.meteors.internal.admin;

import com.github.kagkarlsson.scheduler.Scheduler;
import io.nextskip.common.admin.AbstractScheduledAdminProvider;
import io.nextskip.common.admin.ScheduledFeedDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Provides admin status for meteors module feeds.
 *
 * <p>Exposes status for:
 * <ul>
 *   <li>Meteor Showers - Meteor shower visibility data (1 hour refresh)</li>
 * </ul>
 */
@Component
@ConditionalOnBean(Scheduler.class)
public class MeteorsAdminProvider extends AbstractScheduledAdminProvider {

    private static final String MODULE_NAME = "meteors";

    private static final List<ScheduledFeedDefinition> FEEDS = List.of(
            new ScheduledFeedDefinition(
                    "Meteor Showers",
                    "meteor-refresh",
                    Duration.ofHours(1).toSeconds()
            )
    );

    public MeteorsAdminProvider(Scheduler scheduler) {
        super(scheduler, MODULE_NAME, FEEDS);
    }
}
