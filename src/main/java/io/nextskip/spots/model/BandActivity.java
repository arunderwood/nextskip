package io.nextskip.spots.model;

import io.nextskip.common.api.Scoreable;

import java.time.Instant;
import java.util.Set;

/**
 * Aggregated band activity data for a specific amateur radio band.
 *
 * <p>This model represents a snapshot of activity on a band over a mode-appropriate
 * time window. It includes spot counts, trend analysis, DX reach, and path activity
 * to help operators identify the "hottest" bands for current conditions.
 *
 * <p>Scoring is weighted across multiple factors:
 * <ul>
 *   <li>40% - Activity level (spot count normalized)</li>
 *   <li>30% - Trend (positive trend indicates improving conditions)</li>
 *   <li>20% - DX reach (longer distances indicate better propagation)</li>
 *   <li>10% - Path diversity (more active paths = more opportunities)</li>
 * </ul>
 *
 * @param band the band name (e.g., "20m", "40m")
 * @param mode the primary mode detected on this band (e.g., "FT8", "CW")
 * @param spotCount number of spots in the current window
 * @param baselineSpotCount rolling average of prior windows
 * @param trendPercentage percent change vs baseline (-100 to +infinity)
 * @param maxDxKm maximum distance spotted in the window (km), null if no DX data
 * @param maxDxPath description of max DX path (e.g., "JA1ABC → W6XYZ"), null if no DX
 * @param activePaths set of major HF paths with activity above threshold
 * @param windowStart start of the current aggregation window
 * @param windowEnd end of the current aggregation window
 * @param calculatedAt timestamp when this aggregation was computed
 */
public record BandActivity(
        String band,
        String mode,
        int spotCount,
        int baselineSpotCount,
        double trendPercentage,
        Integer maxDxKm,
        String maxDxPath,
        Set<ContinentPath> activePaths,
        Instant windowStart,
        Instant windowEnd,
        Instant calculatedAt
) implements Scoreable {

    // Scoring thresholds
    private static final int HIGH_ACTIVITY_THRESHOLD = 100;
    private static final int MEDIUM_ACTIVITY_THRESHOLD = 50;
    private static final int LOW_ACTIVITY_THRESHOLD = 10;

    private static final double STRONG_POSITIVE_TREND = 50.0;
    private static final double POSITIVE_TREND = 20.0;

    private static final int EXCELLENT_DX_KM = 10_000;
    private static final int GOOD_DX_KM = 5_000;
    private static final int MODERATE_DX_KM = 2_000;

    private static final int MANY_PATHS = 4;
    private static final int SOME_PATHS = 2;
    private static final int SINGLE_PATH = 1;

    // Scoring weights
    private static final double ACTIVITY_WEIGHT = 0.40;
    private static final double TREND_WEIGHT = 0.30;
    private static final double DX_WEIGHT = 0.20;
    private static final double PATH_WEIGHT = 0.10;

    /**
     * Compact constructor with defensive copying for the paths set.
     */
    public BandActivity {
        activePaths = activePaths != null ? Set.copyOf(activePaths) : Set.of();
    }

    /**
     * Band conditions are favorable when there's high activity with positive trend
     * and at least one active DX path.
     *
     * @return true if conditions suggest a band opening worth monitoring
     */
    @Override
    public boolean isFavorable() {
        return spotCount >= HIGH_ACTIVITY_THRESHOLD
                && trendPercentage > 0
                && !activePaths.isEmpty();
    }

    /**
     * Calculate a score from 0-100 based on multiple factors.
     *
     * <p>The score is a weighted combination of:
     * <ul>
     *   <li>40% - Activity (spot count)</li>
     *   <li>30% - Trend (positive momentum)</li>
     *   <li>20% - DX reach (distance)</li>
     *   <li>10% - Path diversity</li>
     * </ul>
     *
     * @return normalized score from 0-100
     */
    @Override
    public int getScore() {
        int activityScore = normalizeActivity();
        int trendScore = normalizeTrend();
        int dxScore = normalizeDx();
        int pathScore = normalizePaths();

        double weightedScore = activityScore * ACTIVITY_WEIGHT
                + trendScore * TREND_WEIGHT
                + dxScore * DX_WEIGHT
                + pathScore * PATH_WEIGHT;

        return (int) Math.round(Math.min(100, Math.max(0, weightedScore)));
    }

    /**
     * Normalize activity level to 0-100 score.
     *
     * <p>Scoring tiers:
     * <ul>
     *   <li>100+ spots: 100 points</li>
     *   <li>50-100 spots: 50-100 points (linear)</li>
     *   <li>10-50 spots: 20-50 points (linear)</li>
     *   <li>0-10 spots: 0-20 points (linear)</li>
     * </ul>
     */
    private int normalizeActivity() {
        if (spotCount >= HIGH_ACTIVITY_THRESHOLD) {
            return 100;
        } else if (spotCount >= MEDIUM_ACTIVITY_THRESHOLD) {
            // Linear interpolation from 50 to 100 as count goes from 50 to 100
            return 50 + (spotCount - MEDIUM_ACTIVITY_THRESHOLD);
        } else if (spotCount >= LOW_ACTIVITY_THRESHOLD) {
            // Linear interpolation from 20 to 50 as count goes from 10 to 50
            double ratio = (double) (spotCount - LOW_ACTIVITY_THRESHOLD)
                    / (MEDIUM_ACTIVITY_THRESHOLD - LOW_ACTIVITY_THRESHOLD);
            return (int) (20 + ratio * 30);
        } else {
            // Linear interpolation from 0 to 20 as count goes from 0 to 10
            return spotCount * 2;
        }
    }

    /**
     * Normalize trend percentage to 0-100 score.
     *
     * <p>Scoring:
     * <ul>
     *   <li>50%+ increase: 100 points (strong opening)</li>
     *   <li>20-50% increase: 70-100 points</li>
     *   <li>0-20% increase: 50-70 points</li>
     *   <li>0% (flat): 50 points (baseline)</li>
     *   <li>Negative: 0-50 points (declining)</li>
     * </ul>
     */
    private int normalizeTrend() {
        if (trendPercentage >= STRONG_POSITIVE_TREND) {
            return 100;
        } else if (trendPercentage >= POSITIVE_TREND) {
            // Linear from 70 to 100 as trend goes from 20 to 50
            double ratio = (trendPercentage - POSITIVE_TREND)
                    / (STRONG_POSITIVE_TREND - POSITIVE_TREND);
            return (int) (70 + ratio * 30);
        } else if (trendPercentage >= 0) {
            // Linear from 50 to 70 as trend goes from 0 to 20
            double ratio = trendPercentage / POSITIVE_TREND;
            return (int) (50 + ratio * 20);
        } else {
            // Negative trend: decay toward 0
            // At -100% trend, score is 0
            return (int) Math.max(0, 50 + trendPercentage / 2);
        }
    }

    /**
     * Normalize DX distance to 0-100 score.
     *
     * <p>Scoring:
     * <ul>
     *   <li>10,000+ km: 100 points (excellent DX)</li>
     *   <li>5,000-10,000 km: 70-100 points</li>
     *   <li>2,000-5,000 km: 40-70 points</li>
     *   <li>0-2,000 km: 0-40 points</li>
     *   <li>null (no data): 0 points</li>
     * </ul>
     */
    private int normalizeDx() {
        if (maxDxKm == null || maxDxKm <= 0) {
            return 0;
        } else if (maxDxKm >= EXCELLENT_DX_KM) {
            return 100;
        } else if (maxDxKm >= GOOD_DX_KM) {
            double ratio = (double) (maxDxKm - GOOD_DX_KM)
                    / (EXCELLENT_DX_KM - GOOD_DX_KM);
            return (int) (70 + ratio * 30);
        } else if (maxDxKm >= MODERATE_DX_KM) {
            double ratio = (double) (maxDxKm - MODERATE_DX_KM)
                    / (GOOD_DX_KM - MODERATE_DX_KM);
            return (int) (40 + ratio * 30);
        } else {
            double ratio = (double) maxDxKm / MODERATE_DX_KM;
            return (int) (ratio * 40);
        }
    }

    /**
     * Normalize path count to 0-100 score.
     *
     * <p>Scoring based on number of active major paths:
     * <ul>
     *   <li>4+ paths: 100 points (band is wide open)</li>
     *   <li>2-3 paths: 50-100 points</li>
     *   <li>1 path: 30 points</li>
     *   <li>0 paths: 0 points</li>
     * </ul>
     */
    private int normalizePaths() {
        int pathCount = activePaths.size();

        if (pathCount >= MANY_PATHS) {
            return 100;
        } else if (pathCount >= SOME_PATHS) {
            // Linear from 50 to 100 as paths go from 2 to 4
            return 50 + (pathCount - SOME_PATHS) * 25;
        } else if (pathCount == SINGLE_PATH) {
            return 30;
        } else {
            return 0;
        }
    }

    /**
     * Returns the duration of the aggregation window in minutes.
     *
     * @return window duration in minutes
     */
    public long getWindowMinutes() {
        if (windowStart == null || windowEnd == null) {
            return 0;
        }
        return java.time.Duration.between(windowStart, windowEnd).toMinutes();
    }

    /**
     * Checks if there is any activity on this band.
     *
     * @return true if at least one spot exists in the window
     */
    public boolean hasActivity() {
        return spotCount > 0;
    }

    /**
     * Checks if DX (long-distance) data is available.
     *
     * @return true if maxDxKm is populated
     */
    public boolean hasDxData() {
        return maxDxKm != null && maxDxKm > 0;
    }
}
