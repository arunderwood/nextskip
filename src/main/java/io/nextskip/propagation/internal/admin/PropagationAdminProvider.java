package io.nextskip.propagation.internal.admin;

import com.github.kagkarlsson.scheduler.Scheduler;
import io.nextskip.common.admin.AbstractScheduledAdminProvider;
import io.nextskip.common.admin.ScheduledFeedDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

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
public class PropagationAdminProvider extends AbstractScheduledAdminProvider {

    private static final String MODULE_NAME = "propagation";

    private static final List<ScheduledFeedDefinition> FEEDS = List.of(
            new ScheduledFeedDefinition(
                    "NOAA SWPC",
                    "noaa-refresh",
                    Duration.ofMinutes(5).toSeconds()
            ),
            new ScheduledFeedDefinition(
                    "HamQSL Solar",
                    "hamqsl-solar-refresh",
                    Duration.ofMinutes(30).toSeconds()
            ),
            new ScheduledFeedDefinition(
                    "HamQSL Band",
                    "hamqsl-band-refresh",
                    Duration.ofMinutes(15).toSeconds()
            )
    );

    public PropagationAdminProvider(Scheduler scheduler) {
        super(scheduler, MODULE_NAME, FEEDS);
    }
}
