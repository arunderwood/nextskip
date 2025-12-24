package io.nextskip.propagation.model;

import io.nextskip.common.api.Scoreable;

import java.time.Instant;

/**
 * Solar activity indices used for HF propagation forecasting.
 *
 * @param solarFluxIndex      Solar Flux Index (SFI) at 10.7cm, typically 50-300
 * @param aIndex              Planetary A-index (geomagnetic activity), 0-400
 * @param kIndex              Planetary K-index (geomagnetic activity), 0-9
 * @param sunspotNumber       Smoothed sunspot number
 * @param timestamp           When these indices were observed
 * @param source              Data source (e.g., "NOAA SWPC", "HamQSL")
 */
public record SolarIndices(
        double solarFluxIndex,
        int aIndex,
        int kIndex,
        int sunspotNumber,
        Instant timestamp,
        String source
) implements Scoreable {

    // Geomagnetic activity thresholds (K-index scale 0-9)
    private static final int K_INDEX_QUIET_MAX = 2;
    private static final int K_INDEX_UNSETTLED_MAX = 4;
    private static final int K_INDEX_ACTIVE_MAX = 6;
    private static final int K_INDEX_STORM_MAX = 8;

    // Solar flux thresholds (typical range 50-300 SFU)
    private static final double SFI_VERY_LOW_MAX = 70.0;
    private static final double SFI_LOW_MAX = 100.0;
    private static final double SFI_MODERATE_MAX = 150.0;
    private static final double SFI_HIGH_MAX = 200.0;

    /**
     * Determine if conditions are generally favorable for HF propagation.
     *
     * Good conditions: High SFI (>100), low K-index (<4), low A-index (<20)
     */
    @Override
    public boolean isFavorable() {
        return solarFluxIndex > SFI_LOW_MAX && kIndex < K_INDEX_UNSETTLED_MAX && aIndex < 20;
    }

    /**
     * Calculate a 0-100 score based on solar conditions.
     *
     * <p>Score is weighted combination of:
     * <ul>
     *     <li>60% Solar Flux (normalized to 0-100 from typical 50-200 range)</li>
     *     <li>30% K-Index (inverted - lower is better)</li>
     *     <li>10% A-Index (inverted - lower is better)</li>
     * </ul>
     *
     * @return score from 0-100 representing overall propagation quality
     */
    @Override
    public int getScore() {
        // Normalize SFI from typical 50-200 range to 0-100
        double sfiScore = Math.max(0, Math.min(100, ((solarFluxIndex - 50) / 150.0) * 100));

        // K-index: 0-9 scale, invert it (0 is best, 9 is worst)
        double kScore = Math.max(0, (9 - kIndex) / 9.0 * 100);

        // A-index: typical range 0-50, invert it (0 is best)
        double aScore = Math.max(0, Math.min(100, (50 - Math.min(aIndex, 50)) / 50.0 * 100));

        // Weighted combination
        return (int) Math.round(sfiScore * 0.6 + kScore * 0.3 + aScore * 0.1);
    }

    /**
     * Get a human-readable description of current geomagnetic activity.
     */
    public String getGeomagneticActivity() {
        if (kIndex <= K_INDEX_QUIET_MAX) {
            return "Quiet";
        } else if (kIndex <= K_INDEX_UNSETTLED_MAX) {
            return "Unsettled";
        } else if (kIndex <= K_INDEX_ACTIVE_MAX) {
            return "Active";
        } else if (kIndex <= K_INDEX_STORM_MAX) {
            return "Storm";
        } else {
            return "Severe Storm";
        }
    }

    /**
     * Get a description of solar flux level.
     */
    public String getSolarFluxLevel() {
        if (solarFluxIndex < SFI_VERY_LOW_MAX) {
            return "Very Low";
        } else if (solarFluxIndex < SFI_LOW_MAX) {
            return "Low";
        } else if (solarFluxIndex < SFI_MODERATE_MAX) {
            return "Moderate";
        } else if (solarFluxIndex < SFI_HIGH_MAX) {
            return "High";
        } else {
            return "Very High";
        }
    }
}
