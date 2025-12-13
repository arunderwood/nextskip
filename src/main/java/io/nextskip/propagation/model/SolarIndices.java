package io.nextskip.propagation.model;

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
) {
    /**
     * Determine if conditions are generally favorable for HF propagation.
     *
     * Good conditions: High SFI (>100), low K-index (<4), low A-index (<20)
     */
    public boolean isFavorable() {
        return solarFluxIndex > 100 && kIndex < 4 && aIndex < 20;
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
