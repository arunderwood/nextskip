package io.nextskip.meteors.api;

import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import io.nextskip.common.model.EventStatus;
import io.nextskip.meteors.model.MeteorShower;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

/**
 * Hilla endpoint for meteor shower data.
 *
 * <p>Provides browser-callable methods for the React dashboard.
 * Hilla automatically generates TypeScript clients for these methods.
 */
@BrowserCallable
@AnonymousAllowed
public class MeteorEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(MeteorEndpoint.class);

    private final MeteorService meteorService;

    public MeteorEndpoint(MeteorService meteorService) {
        this.meteorService = meteorService;
    }

    /**
     * Get meteor shower data for the dashboard.
     *
     * @return MeteorShowersResponse with active and upcoming showers
     */
    public MeteorShowersResponse getMeteorShowers() {
        LOG.debug("Fetching meteor shower data for dashboard");

        List<MeteorShower> showers = meteorService.getMeteorShowers();

        int activeCount = (int) showers.stream()
                .filter(s -> s.getStatus() == EventStatus.ACTIVE)
                .count();

        int upcomingCount = (int) showers.stream()
                .filter(s -> s.getStatus() == EventStatus.UPCOMING)
                .count();

        MeteorShower primary = meteorService.getPrimaryShower().orElse(null);

        MeteorShowersResponse response = new MeteorShowersResponse(
                showers,
                activeCount,
                upcomingCount,
                primary,
                Instant.now()
        );

        LOG.debug("Returning meteor data: {} active, {} upcoming",
                activeCount, upcomingCount);

        return response;
    }
}
