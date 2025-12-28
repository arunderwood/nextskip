package io.nextskip.contests.api;

import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        return contestService.getContestsResponse();
    }
}
