package io.nextskip.spots.api;

import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import io.nextskip.spots.model.BandActivity;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Hilla endpoint for spot and band activity data.
 *
 * <p>Provides browser-callable methods for the React dashboard.
 * Hilla automatically generates TypeScript clients for these methods.
 *
 * <p>All methods are marked {@code @AnonymousAllowed} since this is a public dashboard.
 *
 * <p>This endpoint is only created when {@code nextskip.spots.enabled=true}.
 */
@BrowserCallable
@AnonymousAllowed
@ConditionalOnProperty(prefix = "nextskip.spots", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SpotsEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(SpotsEndpoint.class);

    private final SpotsService spotsService;

    /** Creates a new SpotsEndpoint with the given service. */
    public SpotsEndpoint(SpotsService spotsService) {
        this.spotsService = spotsService;
    }

    /**
     * Get current band activity data for all bands.
     *
     * <p>This is the main endpoint called by the React dashboard.
     * Returns aggregated activity for all bands with recent spots.
     *
     * @return BandActivityResponse with current band activities
     */
    public BandActivityResponse getBandActivity() {
        LOG.debug("Fetching band activity for dashboard");
        return spotsService.getBandActivityResponse();
    }

    /**
     * Get activity for a specific band across all active modes.
     *
     * @param band the band name (e.g., "20m", "40m")
     * @return list of activity data per mode, empty if no recent activity
     */
    public List<BandActivity> getBandActivityForBand(String band) {
        LOG.debug("Fetching activity for band: {}", band);
        return spotsService.getBandActivity(band);
    }

    /**
     * Get system status information.
     *
     * <p>Returns MQTT connection status and processing statistics.
     *
     * @return SpotsStatusResponse with system status
     */
    public SpotsStatusResponse getStatus() {
        LOG.debug("Fetching spots system status");
        return new SpotsStatusResponse(
                spotsService.isConnected(),
                spotsService.getSourceName(),
                spotsService.getSpotCount(),
                spotsService.getLastSpotTime().orElse(null),
                spotsService.getSpotsProcessed()
        );
    }
}
