package io.nextskip.spots.api;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nextskip.spots.internal.client.SpotSource;
import io.nextskip.spots.internal.stream.SpotStreamProcessor;
import io.nextskip.spots.persistence.repository.SpotRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Public API for spot status and statistics.
 *
 * <p>Provides minimal status endpoints for Phase 1:
 * <ul>
 *   <li>MQTT connection status</li>
 *   <li>Spot count in database</li>
 *   <li>Most recent spot timestamp</li>
 *   <li>Processing statistics</li>
 * </ul>
 *
 * <p>Phase 2 will add {@code @BrowserCallable} endpoints for frontend access.
 */
@Service
@ConditionalOnProperty(prefix = "nextskip.spots", name = "enabled", havingValue = "true", matchIfMissing = true)
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed beans are intentionally shared")
public class SpotsService {

    private final SpotSource spotSource;
    private final SpotRepository spotRepository;
    private final SpotStreamProcessor streamProcessor;

    public SpotsService(
            SpotSource spotSource,
            SpotRepository spotRepository,
            SpotStreamProcessor streamProcessor) {
        this.spotSource = spotSource;
        this.spotRepository = spotRepository;
        this.streamProcessor = streamProcessor;
    }

    /**
     * Returns whether the MQTT source is currently connected.
     *
     * @return true if connected and receiving data
     */
    public boolean isConnected() {
        return spotSource.isConnected();
    }

    /**
     * Returns the name of the spot source.
     *
     * @return source name (e.g., "PSKReporter MQTT")
     */
    public String getSourceName() {
        return spotSource.getSourceName();
    }

    /**
     * Returns the total count of spots in the database.
     *
     * @return spot count
     */
    public long getSpotCount() {
        return spotRepository.count();
    }

    /**
     * Returns the timestamp of the most recent spot.
     *
     * @return most recent spot time, or empty if no spots exist
     */
    public Optional<Instant> getLastSpotTime() {
        return spotRepository.findTopByOrderBySpottedAtDesc()
                .map(entity -> entity.getSpottedAt());
    }

    /**
     * Returns the total number of spots processed through the pipeline.
     *
     * <p>This count includes spots that were parsed and enriched,
     * regardless of whether they were successfully persisted.
     *
     * @return spots processed count
     */
    public long getSpotsProcessed() {
        return streamProcessor.getSpotsProcessed();
    }

    /**
     * Returns the total number of batches persisted to the database.
     *
     * @return batches persisted count
     */
    public long getBatchesPersisted() {
        return streamProcessor.getBatchesPersisted();
    }

    /**
     * Returns the count of spots received in the last specified minutes.
     *
     * @param minutes the time window in minutes
     * @return count of spots received within the time window
     */
    public long getSpotCountSince(int minutes) {
        Instant cutoff = Instant.now().minusSeconds(minutes * 60L);
        return spotRepository.countByCreatedAtAfter(cutoff);
    }
}
