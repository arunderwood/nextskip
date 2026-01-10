package io.nextskip.activations.internal.admin;

import com.github.kagkarlsson.scheduler.Scheduler;
import io.nextskip.common.admin.AbstractScheduledAdminProvider;
import io.nextskip.common.admin.ScheduledFeedDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

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
public class ActivationsAdminProvider extends AbstractScheduledAdminProvider {

    private static final String MODULE_NAME = "activations";

    private static final List<ScheduledFeedDefinition> FEEDS = List.of(
            new ScheduledFeedDefinition(
                    "POTA",
                    "pota-refresh",
                    Duration.ofMinutes(1).toSeconds()
            ),
            new ScheduledFeedDefinition(
                    "SOTA",
                    "sota-refresh",
                    Duration.ofMinutes(1).toSeconds()
            )
    );

    public ActivationsAdminProvider(Scheduler scheduler) {
        super(scheduler, MODULE_NAME, FEEDS);
    }
}
