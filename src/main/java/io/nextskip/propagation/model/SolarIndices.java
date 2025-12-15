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
    /**
     * Determine if conditions are generally favorable for HF propagation.
     *
     * Good conditions: High SFI (>100), low K-index (<4), low A-index (<20)
     */
    @Override
    public boolean isFavorable() {
        return solarFluxIndex > 100 && kIndex < 4 && aIndex < 20;
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
        if (kIndex <= 2) {
            return "Quiet";
        } else if (kIndex <= 4) {
            return "Unsettled";
        } else if (kIndex <= 6) {
            return "Active";
        } else if (kIndex <= 8) {
            return "Storm";
        } else {
            return "Severe Storm";
        }
    }

    /**
     * Get a description of solar flux level.
     */
    public String getSolarFluxLevel() {
        if (solarFluxIndex < 70) {
            return "Very Low";
        } else if (solarFluxIndex < 100) {
            return "Low";
        } else if (solarFluxIndex < 150) {
            return "Moderate";
        } else if (solarFluxIndex < 200) {
            return "High";
        } else {
            return "Very High";
        }
    }
}
