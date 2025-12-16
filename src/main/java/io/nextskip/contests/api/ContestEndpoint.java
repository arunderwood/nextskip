package io.nextskip.contests.api;

import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import io.nextskip.common.model.EventStatus;
import io.nextskip.contests.model.Contest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Hilla endpoint for contest calendar data.
 *
 * <p>Provides browser-callable methods for the React dashboard.
 * Hilla automatically generates TypeScript clients for these methods.
 *
 * <p>All methods are marked @AnonymousAllowed since this is a public dashboard.
 */
@BrowserCallable
@AnonymousAllowed
public class ContestEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(ContestEndpoint.class);

    private final ContestService contestService;

    public ContestEndpoint(ContestService contestService) {
        this.contestService = contestService;
    }

    /**
     * Get upcoming contest data for the dashboard.
     *
     * <p>This is the main endpoint called by the React dashboard.
     * Returns contests from the WA7BNM calendar (typically 8-day window).
     *
     * @return ContestsResponse with upcoming contest data
     */
    public ContestsResponse getContests() {
        LOG.debug("Fetching contest data for dashboard");

        List<Contest> contests = contestService.getUpcomingContests();

        // Calculate counts by status
        int activeCount = (int) contests.stream()
                .filter(c -> c.getStatus() == EventStatus.ACTIVE)
                .count();

        int upcomingCount = (int) contests.stream()
                .filter(c -> c.getStatus() == EventStatus.UPCOMING)
                .filter(c -> c.getTimeRemaining().compareTo(Duration.ofHours(24)) <= 0)
                .count();

        int totalCount = contests.size();

        ContestsResponse response = new ContestsResponse(
                contests,
                activeCount,
                upcomingCount,
                totalCount,
                Instant.now()
        );

        LOG.debug("Returning contest data: {} active, {} upcoming soon, {} total",
                activeCount, upcomingCount, totalCount);

        return response;
    }
}
