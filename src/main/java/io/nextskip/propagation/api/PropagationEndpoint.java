package io.nextskip.propagation.api;

import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.SolarIndices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Hilla endpoint for propagation data.
 *
 * Provides browser-callable methods for the React dashboard.
 * Hilla automatically generates TypeScript clients for these methods.
 *
 * All methods are marked @AnonymousAllowed since this is a public dashboard.
 */
@BrowserCallable
@AnonymousAllowed
public class PropagationEndpoint {

    private static final Logger log = LoggerFactory.getLogger(PropagationEndpoint.class);

    private final PropagationService propagationService;

    public PropagationEndpoint(PropagationService propagationService) {
        this.propagationService = propagationService;
    }

    /**
     * Get current propagation data including solar indices and band conditions.
     *
     * This is the main endpoint called by the React dashboard.
     *
     * @return PropagationResponse with current data
     */
    public PropagationResponse getPropagationData() {
        log.debug("Fetching propagation data for dashboard");

        SolarIndices solarIndices = propagationService.getCurrentSolarIndices();
        List<BandCondition> bandConditions = propagationService.getBandConditions();

        PropagationResponse response = new PropagationResponse(solarIndices, bandConditions);

        log.debug("Returning propagation data: {} band conditions",
                  bandConditions != null ? bandConditions.size() : 0);

        return response;
    }

    /**
     * Get only solar indices (lighter weight call).
     *
     * @return Current solar indices
     */
    public SolarIndices getSolarIndices() {
        log.debug("Fetching solar indices");
        return propagationService.getCurrentSolarIndices();
    }

    /**
     * Get only band conditions.
     *
     * @return List of band conditions
     */
    public List<BandCondition> getBandConditions() {
        log.debug("Fetching band conditions");
        return propagationService.getBandConditions();
    }
}
