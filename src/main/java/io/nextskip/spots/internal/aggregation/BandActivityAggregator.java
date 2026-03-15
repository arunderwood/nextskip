package io.nextskip.spots.internal.aggregation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nextskip.spots.internal.ScoringProperties;
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
 * <p>The aggregation uses mode-specific windows (FT8/FT4/FT2=15m, CW=30m, SSB=60m)
 * to account for the different activity levels of each mode.
 *
 * <p>Each band can have multiple BandActivity records — one per active mode.
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
     * Minimum window count required to calculate a baseline.
     */
    private static final int MIN_WINDOWS_FOR_BASELINE = 1;

    private final SpotRepository repository;
    private final Clock clock;
    private final ScoringProperties scoringProperties;

    public BandActivityAggregator(SpotRepository repository, Clock clock,
                                  ScoringProperties scoringProperties) {
        this.repository = repository;
        this.clock = clock;
        this.scoringProperties = scoringProperties;
    }

    /**
     * Aggregates activity data for a specific band and mode combination.
     *
     * <p>The aggregation process:
     * <ol>
     *   <li>Selects mode-appropriate time window</li>
     *   <li>Counts spots in current window for the specific mode</li>
     *   <li>Calculates rolling baseline average for the specific mode</li>
     *   <li>Computes trend percentage</li>
     *   <li>Finds maximum DX spot for the specific mode</li>
     *   <li>Identifies active continent paths for the specific mode</li>
     * </ol>
     *
     * @param band the band to aggregate (e.g., "20m")
     * @param mode the mode to aggregate (e.g., "FT4")
     * @return aggregated band activity data for the specific mode
     */
    public BandActivity aggregateBandMode(String band, String mode) {
        Instant now = clock.instant();
        ModeWindow modeWindow = ModeWindow.forMode(mode);

        Instant windowStart = now.minus(modeWindow.getCurrentWindow());

        LOG.debug("Aggregating band {} mode {} window {} from {}",
                band, mode, modeWindow.getCurrentWindow(), windowStart);

        // Current window spot count (mode-filtered)
        int currentCount = (int) repository.countByBandAndModeAndSpottedAtAfter(band, mode, windowStart);

        // Baseline calculation (mode-filtered)
        int baselineCount = calculateBaseline(band, mode, modeWindow, now);

        // Trend percentage
        double trend = calculateTrend(currentCount, baselineCount);

        // Max DX spot (mode-filtered)
        Optional<SpotEntity> maxDxSpot = repository
                .findMaxDxSpotByBandAndModeAndSpottedAtAfter(band, mode, windowStart);
        Integer maxDxKm = maxDxSpot.map(SpotEntity::getDistanceKm).orElse(null);
        String maxDxPath = maxDxSpot
                .map(this::formatDxPath)
                .orElse(null);

        // Active continent paths (mode-filtered)
        Set<ContinentPath> activePaths = findActivePaths(band, mode, windowStart);

        double rarityMultiplier = scoringProperties.getMultiplierForMode(mode);

        BandActivity activity = new BandActivity(
                band,
                mode,
                currentCount,
                baselineCount,
                trend,
                maxDxKm,
                maxDxPath,
                activePaths,
                windowStart,
                now,
                now,
                rarityMultiplier
        );

        LOG.debug("Band {} {} activity: {} spots (baseline: {}), trend: {}%, DX: {} km, paths: {}",
                band, mode, currentCount, baselineCount, String.format("%.1f", trend),
                maxDxKm, activePaths.size());

        return activity;
    }

    /**
     * Aggregates activity data for all band+mode combinations with recent activity.
     *
     * <p>Returns a map with composite {@code "{band}_{mode}"} keys (e.g., "20m_FT8",
     * "20m_FT4") to activity data. Only band+mode pairs with spots in the last
     * 2 hours are included.
     *
     * @return map of composite key to aggregated activity
     */
    public Map<String, BandActivity> aggregateAllBands() {
        long totalStart = System.nanoTime();
        Instant lookback = clock.instant().minus(ACTIVITY_LOOKBACK);
        List<Object[]> activePairs = repository.findDistinctBandModePairsWithActivitySince(lookback);

        LOG.info("Aggregating activity for {} band+mode pairs with recent activity", activePairs.size());

        Map<String, BandActivity> result = new LinkedHashMap<>();
        for (Object[] pair : activePairs) {
            String band = (String) pair[0];
            String mode = (String) pair[1];
            String compositeKey = band + "_" + mode;
            try {
                long pairStart = System.nanoTime();
                BandActivity activity = aggregateBandMode(band, mode);
                long pairMs = (System.nanoTime() - pairStart) / 1_000_000;
                if (pairMs > 1000) {
                    LOG.warn("Slow aggregation: {} took {}ms", compositeKey, pairMs);
                }
                result.put(compositeKey, activity);
            } catch (org.springframework.dao.DataAccessException e) {
                LOG.warn("Failed to aggregate {} {}: {}", band, mode, e.getMessage());
            }
        }

        long totalMs = (System.nanoTime() - totalStart) / 1_000_000;
        LOG.info("Completed aggregation: {} band+mode pairs processed in {}ms", result.size(), totalMs);
        return result;
    }

    /**
     * Calculates the baseline spot count for a specific mode (rolling average of prior windows).
     *
     * <p>Divides the baseline period into current-window-sized chunks
     * and averages them, excluding the current window.
     *
     * <p>Example for FT8 (15m current, 1hr baseline):
     * - Window count = 4 (60/15)
     * - Averages windows: [-15m to -30m], [-30m to -45m], [-45m to -60m]
     * - Current window [0 to -15m] is excluded
     */
    private int calculateBaseline(String band, String mode, ModeWindow modeWindow, Instant now) {
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

            long count = repository.countByBandAndModeAndSpottedAtAfter(band, mode, windowStart)
                    - repository.countByBandAndModeAndSpottedAtAfter(band, mode, windowEnd);

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
     * Finds which major continent paths are active on a band for a specific mode.
     *
     * <p>A path is considered "active" if at least {@link #MIN_SPOTS_FOR_ACTIVE_PATH}
     * spots exist between the two continents in the time window.
     */
    private Set<ContinentPath> findActivePaths(String band, String mode, Instant since) {
        List<Object[]> pathCounts = repository
                .countContinentPathsByBandAndModeAndSpottedAtAfter(band, mode, since);

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
