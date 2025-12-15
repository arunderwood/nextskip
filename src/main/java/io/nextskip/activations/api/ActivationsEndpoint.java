package io.nextskip.activations.api;

import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationsSummary;
import io.nextskip.activations.model.ActivationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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

        ActivationsSummary summary = activationsService.getActivationsSummary();

        // Separate activations by type for the response
        List<Activation> potaActivations = summary.activations().stream()
                .filter(a -> a.type() == ActivationType.POTA)
                .toList();

        List<Activation> sotaActivations = summary.activations().stream()
                .filter(a -> a.type() == ActivationType.SOTA)
                .toList();

        int totalCount = potaActivations.size() + sotaActivations.size();

        ActivationsResponse response = new ActivationsResponse(
                potaActivations,
                sotaActivations,
                totalCount,
                summary.lastUpdated()
        );

        LOG.debug("Returning activations data: {} POTA, {} SOTA (total: {})",
                potaActivations.size(), sotaActivations.size(), totalCount);

        return response;
    }
}
