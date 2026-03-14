package io.nextskip.spots.internal;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Configuration properties for spot scoring, including mode rarity multipliers.
 *
 * <p>Rarity multipliers boost the activity score component for less popular modes
 * so they rank competitively with FT8 activity of comparable propagation quality.
 *
 * <p>Configured via {@code nextskip.spots.scoring} in application.yml:
 * <pre>
 * nextskip:
 *   spots:
 *     scoring:
 *       rarity-multipliers:
 *         FT8: 1.0
 *         FT4: 1.5
 *         FT2: 3.0
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "nextskip.spots.scoring")
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "Spring ConfigurationProperties requires mutable getter/setter for binding")
public class ScoringProperties {

    private static final double DEFAULT_MULTIPLIER = 1.0;

    private Map<String, Double> rarityMultipliers = new HashMap<>();

    public Map<String, Double> getRarityMultipliers() {
        return rarityMultipliers;
    }

    public void setRarityMultipliers(Map<String, Double> rarityMultipliers) {
        this.rarityMultipliers = rarityMultipliers;
    }

    /**
     * Returns the rarity multiplier for a given mode.
     *
     * <p>Lookup is case-insensitive. Defaults to 1.0 for modes not explicitly
     * configured (no boost).
     *
     * @param mode the mode string (e.g., "FT4", "FT2")
     * @return the rarity multiplier (1.0 = no boost)
     */
    public double getMultiplierForMode(String mode) {
        if (mode == null) {
            return DEFAULT_MULTIPLIER;
        }
        return rarityMultipliers.getOrDefault(
                mode.toUpperCase(Locale.ROOT),
                DEFAULT_MULTIPLIER
        );
    }
}
