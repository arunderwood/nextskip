package io.nextskip.spots.internal.aggregation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nextskip.spots.model.BandActivity;
import io.nextskip.spots.model.ContinentPath;
import io.nextskip.spots.model.ModeWindow;
import io.nextskip.spots.persistence.entity.SpotEntity;
import io.nextskip.spots.persistence.repository.SpotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Aggregates spot data into band activity summaries.
 *
 * <p>This service computes aggregated metrics for each amateur radio band:
 * <ul>
 *   <li>Spot counts within mode-appropriate time windows</li>
 *   <li>Trend analysis comparing current to rolling baseline</li>
 *   <li>Maximum DX distance and path details</li>
 *   <li>Active continent-to-continent propagation paths</li>
 * </ul>
 *
 * <p>The aggregation uses mode-specific windows (FT8=15m, CW=30m, SSB=60m)
 * to account for the different activity levels of each mode.
 */
@Service
@ConditionalOnProperty(prefix = "nextskip.spots", name = "enabled", havingValue = "true", matchIfMissing = true)
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed beans are intentionally shared")
public class BandActivityAggregator {

    private static final Logger LOG = LoggerFactory.getLogger(BandActivityAggregator.class);

    /**
     * Minimum spots required to consider a continent path as "active".
     */
    private static final int MIN_SPOTS_FOR_ACTIVE_PATH = 5;

    /**
     * How far back to look for bands with any activity.
     */
    private static final Duration ACTIVITY_LOOKBACK = Duration.ofHours(2);

    /**
     * Duration for determining primary mode on a band.
     */
    private static final Duration MODE_DETECTION_WINDOW = Duration.ofMinutes(30);

    /**
     * Minimum window count required to calculate a baseline.
     */
    private static final int MIN_WINDOWS_FOR_BASELINE = 1;

    /**
     * Default mode if none detected.
     */
    private static final String DEFAULT_MODE = "FT8";

    private final SpotRepository repository;
    private final Clock clock;

    public BandActivityAggregator(SpotRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Aggregates activity data for a specific band.
     *
     * <p>The aggregation process:
     * <ol>
     *   <li>Determines primary mode (most common in last 30 minutes)</li>
     *   <li>Selects mode-appropriate time window</li>
     *   <li>Counts spots in current window</li>
     *   <li>Calculates rolling baseline average</li>
     *   <li>Computes trend percentage</li>
     *   <li>Finds maximum DX spot</li>
     *   <li>Identifies active continent paths</li>
     * </ol>
     *
     * @param band the band to aggregate (e.g., "20m")
     * @return aggregated band activity data
     */
    public BandActivity aggregateBand(String band) {
        Instant now = clock.instant();

        // Determine primary mode and its window configuration
        String primaryMode = determinePrimaryMode(band, now);
        ModeWindow modeWindow = ModeWindow.forMode(primaryMode);

        Instant windowStart = now.minus(modeWindow.getCurrentWindow());

        LOG.debug("Aggregating band {} with mode {} window {} from {}",
                band, primaryMode, modeWindow.getCurrentWindow(), windowStart);

        // Current window spot count
        int currentCount = (int) repository.countByBandAndSpottedAtAfter(band, windowStart);

        // Baseline calculation (rolling average of prior windows)
        int baselineCount = calculateBaseline(band, modeWindow, now);

        // Trend percentage
        double trend = calculateTrend(currentCount, baselineCount);

        // Max DX spot
        Optional<SpotEntity> maxDxSpot = repository
                .findMaxDxSpotByBandAndSpottedAtAfter(band, windowStart);
        Integer maxDxKm = maxDxSpot.map(SpotEntity::getDistanceKm).orElse(null);
        String maxDxPath = maxDxSpot
                .map(this::formatDxPath)
                .orElse(null);

        // Active continent paths
        Set<ContinentPath> activePaths = findActivePaths(band, windowStart);

        BandActivity activity = new BandActivity(
                band,
                primaryMode,
                currentCount,
                baselineCount,
                trend,
                maxDxKm,
                maxDxPath,
                activePaths,
                windowStart,
                now,
                now
        );

        LOG.debug("Band {} activity: {} spots (baseline: {}), trend: {}%, DX: {} km, paths: {}",
                band, currentCount, baselineCount, String.format("%.1f", trend), maxDxKm, activePaths.size());

        return activity;
    }

    /**
     * Aggregates activity data for all bands with recent activity.
     *
     * <p>Returns a map of band name to activity data, ordered by band name.
     * Only bands with spots in the last 2 hours are included.
     *
     * @return map of band name to aggregated activity
     */
    public Map<String, BandActivity> aggregateAllBands() {
        Instant lookback = clock.instant().minus(ACTIVITY_LOOKBACK);
        List<String> activeBands = repository.findDistinctBandsWithActivitySince(lookback);

        LOG.info("Aggregating activity for {} bands with recent activity", activeBands.size());

        Map<String, BandActivity> result = new LinkedHashMap<>();
        for (String band : activeBands) {
            try {
                BandActivity activity = aggregateBand(band);
                result.put(band, activity);
            } catch (org.springframework.dao.DataAccessException e) {
                LOG.warn("Failed to aggregate band {}: {}", band, e.getMessage());
            }
        }

        LOG.info("Completed aggregation: {} bands processed", result.size());
        return result;
    }

    /**
     * Determines the primary (most common) mode on a band.
     *
     * <p>Looks at the last 30 minutes of spots and returns the mode with
     * the highest count. Defaults to "FT8" if no spots found.
     */
    private String determinePrimaryMode(String band, Instant now) {
        Instant lookback = now.minus(MODE_DETECTION_WINDOW);
        List<Object[]> distribution = repository.findModeDistributionByBandSince(band, lookback);

        if (distribution.isEmpty()) {
            return DEFAULT_MODE;
        }

        // First row has highest count due to ORDER BY cnt DESC
        return (String) distribution.get(0)[0];
    }

    /**
     * Calculates the baseline spot count (rolling average of prior windows).
     *
     * <p>Divides the baseline period into current-window-sized chunks
     * and averages them, excluding the current window.
     *
     * <p>Example for FT8 (15m current, 1hr baseline):
     * - Window count = 4 (60/15)
     * - Averages windows: [-15m to -30m], [-30m to -45m], [-45m to -60m]
     * - Current window [0 to -15m] is excluded
     */
    private int calculateBaseline(String band, ModeWindow modeWindow, Instant now) {
        Duration current = modeWindow.getCurrentWindow();
        int windowCount = modeWindow.getBaselineWindowCount();

        if (windowCount <= MIN_WINDOWS_FOR_BASELINE) {
            return 0;
        }

        // Average the prior windows (skip current window at index 0)
        long totalSpots = 0;
        int validWindows = 0;

        for (int i = 1; i < windowCount; i++) {
            Instant windowEnd = now.minus(current.multipliedBy(i));
            Instant windowStart = windowEnd.minus(current);

            long count = repository.countByBandAndSpottedAtAfter(band, windowStart)
                    - repository.countByBandAndSpottedAtAfter(band, windowEnd);

            // Only count if we got a valid result (count could be negative due to timing)
            if (count >= 0) {
                totalSpots += count;
                validWindows++;
            }
        }

        if (validWindows == 0) {
            return 0;
        }

        return (int) (totalSpots / validWindows);
    }

    /**
     * Calculates the trend percentage comparing current to baseline.
     *
     * @return percentage change (-100 to +infinity)
     */
    private double calculateTrend(int current, int baseline) {
        if (baseline == 0) {
            // No baseline data - if we have current activity, show positive trend
            return current > 0 ? 100.0 : 0.0;
        }
        return ((double) (current - baseline) / baseline) * 100.0;
    }

    /**
     * Finds which major continent paths are active on a band.
     *
     * <p>A path is considered "active" if at least {@link #MIN_SPOTS_FOR_ACTIVE_PATH}
     * spots exist between the two continents in the time window.
     */
    private Set<ContinentPath> findActivePaths(String band, Instant since) {
        List<Object[]> pathCounts = repository.countContinentPathsByBandAndSpottedAtAfter(band, since);

        Set<ContinentPath> active = EnumSet.noneOf(ContinentPath.class);
        for (Object[] row : pathCounts) {
            String c1 = (String) row[0];
            String c2 = (String) row[1];
            long count = (Long) row[2];

            if (count >= MIN_SPOTS_FOR_ACTIVE_PATH) {
                ContinentPath.fromContinents(c1, c2).ifPresent(active::add);
            }
        }
        return active;
    }

    /**
     * Formats the DX path string from a spot entity.
     *
     * @return formatted path (e.g., "JA1ABC → W6XYZ")
     */
    private String formatDxPath(SpotEntity spot) {
        String spotted = spot.getSpottedCall();
        String spotter = spot.getSpotterCall();

        if (spotted == null || spotter == null) {
            return null;
        }

        return spotted.toUpperCase(Locale.ROOT) + " → " + spotter.toUpperCase(Locale.ROOT);
    }
}
