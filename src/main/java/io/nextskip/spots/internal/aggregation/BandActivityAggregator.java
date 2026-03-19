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
import java.util.LinkedHashSet;
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
     * <p>Uses 3 bulk SQL queries instead of per-pair N+1 queries:
     * <ol>
     *   <li>Spot counts in 15-minute buckets (replaces ~271 COUNT queries)</li>
     *   <li>Max DX per band+mode via window function (replaces ~38 correlated subqueries)</li>
     *   <li>Continent paths per band+mode (replaces ~38 GROUP BY queries)</li>
     * </ol>
     *
     * <p>Results are assembled into {@link BandActivity} records in Java.
     *
     * @return map of composite key to aggregated activity
     */
    public Map<String, BandActivity> aggregateAllBands() {
        Instant now = clock.instant();
        // 3h covers SSB's baseline window (the widest); 1h covers SSB's current window (the widest)
        Instant baselineLookback = now.minus(Duration.ofHours(3));
        Instant currentLookback = now.minus(Duration.ofHours(1));

        LOG.info("Running bulk aggregation queries");

        List<Object[]> bucketRows = repository.countSpotsByBandModeInBuckets(baselineLookback);
        List<Object[]> dxRows = repository.findMaxDxSpotPerBandMode(currentLookback);
        Map<Duration, Map<String, List<Object[]>>> pathsByDuration = queryPathsByWindowDuration(now);

        LOG.info("Bulk queries complete: {} bucket rows, {} DX rows, {} path duration groups",
                bucketRows.size(), dxRows.size(), pathsByDuration.size());

        // Index all bulk results by composite "band_mode" key for O(1) lookup during assembly
        Map<String, Object[]> dxByKey = new LinkedHashMap<>();
        for (Object[] row : dxRows) {
            dxByKey.put(row[0] + "_" + row[1], row);
        }

        // 15-minute buckets are mode-agnostic; each mode's window config selects
        // which buckets to sum in countSpotsInWindow() / calculateBaselineFromBuckets()
        Map<String, Map<Instant, Long>> bucketsByKey = new LinkedHashMap<>();
        Set<String> activePairKeys = new LinkedHashSet<>();
        for (Object[] row : bucketRows) {
            String key = row[0] + "_" + row[1];
            Instant bucketStart = (Instant) row[2];
            long count = ((Number) row[3]).longValue();
            activePairKeys.add(key);
            bucketsByKey.computeIfAbsent(key, k -> new LinkedHashMap<>())  // NOPMD - one map per band_mode key
                    .put(bucketStart, count);
        }

        Map<String, BandActivity> result = new LinkedHashMap<>();
        for (String key : activePairKeys) {
            result.put(key, assembleBandActivity(key, now, bucketsByKey, dxByKey, pathsByDuration));
        }

        LOG.info("Completed aggregation: {} band+mode pairs processed", result.size());
        return result;
    }

    /**
     * Queries continent paths once per distinct mode window duration.
     *
     * <p>Instead of a single 1-hour lookback for all modes, this runs one query
     * per unique window duration (15m, 30m, 60m) so that continent paths
     * align with each mode's spot counting window.
     *
     * @param now current time
     * @return map from window duration to indexed path results (keyed by band_mode)
     */
    private Map<Duration, Map<String, List<Object[]>>> queryPathsByWindowDuration(Instant now) {
        Map<Duration, Map<String, List<Object[]>>> result = new LinkedHashMap<>();
        for (Duration duration : ModeWindow.distinctCurrentWindows().keySet()) {
            Instant since = now.minus(duration);
            List<Object[]> rows = repository.countContinentPathsPerBandMode(since);
            result.put(duration, indexByBandModeKey(rows));
        }
        return result;
    }

    /**
     * Counts spots within a time window by summing matching 15-minute buckets.
     */
    private int countSpotsInWindow(Map<Instant, Long> buckets, Instant windowStart, Instant windowEnd) {
        long total = 0;
        for (Map.Entry<Instant, Long> entry : buckets.entrySet()) {
            Instant bucketStart = entry.getKey();
            // Bucket is within window if it starts at or after windowStart and before windowEnd
            if (!bucketStart.isBefore(windowStart) && bucketStart.isBefore(windowEnd)) {
                total += entry.getValue();
            }
        }
        return (int) total;
    }

    /**
     * Calculates baseline from pre-bucketed data instead of individual DB queries.
     */
    private int calculateBaselineFromBuckets(Map<Instant, Long> buckets, ModeWindow modeWindow, Instant now) {
        Duration current = modeWindow.getCurrentWindow();
        int windowCount = modeWindow.getBaselineWindowCount();

        if (windowCount <= MIN_WINDOWS_FOR_BASELINE) {
            return 0;
        }

        long totalSpots = 0;
        int validWindows = 0;

        for (int i = 1; i < windowCount; i++) {
            Instant windowEnd = now.minus(current.multipliedBy(i));
            Instant windowStart = windowEnd.minus(current);

            int count = countSpotsInWindow(buckets, windowStart, windowEnd);
            if (count >= 0) {
                totalSpots += count;
                validWindows++;
            }
        }

        return validWindows == 0 ? 0 : (int) (totalSpots / validWindows);
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

    // --- Bulk query result helpers (used by aggregateAllBands) ---

    private BandActivity assembleBandActivity(String key, Instant now,
            Map<String, Map<Instant, Long>> bucketsByKey,
            Map<String, Object[]> dxByKey,
            Map<Duration, Map<String, List<Object[]>>> pathsByDuration) {
        String[] parts = key.split("_", 2);
        String band = parts[0];
        String mode = parts[1];

        ModeWindow modeWindow = ModeWindow.forMode(mode);
        Instant windowStart = now.minus(modeWindow.getCurrentWindow());

        Map<Instant, Long> buckets = bucketsByKey.getOrDefault(key, Map.of());
        int currentCount = countSpotsInWindow(buckets, windowStart, now);
        int baselineCount = calculateBaselineFromBuckets(buckets, modeWindow, now);
        double trend = calculateTrend(currentCount, baselineCount);

        Map<String, List<Object[]>> pathsByKey =
                pathsByDuration.getOrDefault(modeWindow.getCurrentWindow(), Map.of());
        Object[] dxRow = dxByKey.get(key);
        return new BandActivity(band, mode, currentCount, baselineCount, trend,
                extractDxDistance(dxRow), extractDxPath(dxRow),
                extractActivePaths(pathsByKey.getOrDefault(key, List.of())),
                windowStart, now, now, scoringProperties.getMultiplierForMode(mode));
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // One list per unique key is intentional
    private Map<String, List<Object[]>> indexByBandModeKey(List<Object[]> rows) {
        Map<String, List<Object[]>> indexed = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String key = row[0] + "_" + row[1];
            indexed.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(row);
        }
        return indexed;
    }

    @SuppressWarnings("PMD.UseVarargs") // Array from native query result, not a varargs call site
    private Integer extractDxDistance(Object[] dxRow) {
        if (dxRow == null) {
            return null;
        }
        Number distance = (Number) dxRow[2];
        return distance != null ? distance.intValue() : null;
    }

    @SuppressWarnings("PMD.UseVarargs") // Array from native query result, not a varargs call site
    private String extractDxPath(Object[] dxRow) {
        if (dxRow == null) {
            return null;
        }
        Number distance = (Number) dxRow[2];
        if (distance == null) {
            return null;
        }
        String spottedCall = (String) dxRow[3];
        String spotterCall = (String) dxRow[4];
        if (spottedCall == null || spotterCall == null) {
            return null;
        }
        return spottedCall.toUpperCase(Locale.ROOT) + " → " + spotterCall.toUpperCase(Locale.ROOT);
    }

    private Set<ContinentPath> extractActivePaths(List<Object[]> pathRows) {
        Set<ContinentPath> active = EnumSet.noneOf(ContinentPath.class);
        for (Object[] row : pathRows) {
            String c1 = (String) row[2];
            String c2 = (String) row[3];
            long count = ((Number) row[4]).longValue();
            if (count >= MIN_SPOTS_FOR_ACTIVE_PATH) {
                ContinentPath.fromContinents(c1, c2).ifPresent(active::add);
            }
        }
        return active;
    }
}
