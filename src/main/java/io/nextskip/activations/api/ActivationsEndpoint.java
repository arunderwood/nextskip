package io.nextskip.activations.api;

import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hilla endpoint for activations data (POTA/SOTA).
 *
 * <p>Provides browser-callable methods for the React dashboard.
 * Hilla automatically generates TypeScript clients for these methods.
 *
 * <p>All methods are marked @AnonymousAllowed since this is a public dashboard.
 */
@BrowserCallable
@AnonymousAllowed
public class ActivationsEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(ActivationsEndpoint.class);

    private final ActivationsService activationsService;

    public ActivationsEndpoint(ActivationsService activationsService) {
        this.activationsService = activationsService;
    }

    /**
     * Get current activations data for POTA and SOTA.
     *
     * <p>This is the main endpoint called by the React dashboard.
     *
     * @return ActivationsResponse with current activation data
     */
    public ActivationsResponse getActivations() {
        LOG.debug("Fetching activations data for dashboard");
        return activationsService.getActivationsResponse();
    }
}
