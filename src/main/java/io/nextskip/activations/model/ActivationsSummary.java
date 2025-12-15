package io.nextskip.activations.model;

import io.nextskip.common.api.Scoreable;
import java.time.Instant;
import java.util.List;

/**
 * Summary of all current activations (POTA and SOTA combined).
 *
 * <p>Provides aggregate scoring for dashboard card prioritization based on
 * the number of active activations and their recency.</p>
 *
 * @param activations List of all current activations
 * @param potaCount Number of POTA activations
 * @param sotaCount Number of SOTA activations
 * @param lastUpdated Timestamp when this summary was generated
 */
public record ActivationsSummary(
        List<Activation> activations,
        int potaCount,
        int sotaCount,
        Instant lastUpdated
) implements Scoreable {

    /**
     * An activations summary is favorable when there are 5 or more total activations.
     *
     * @return true if there are at least 5 activations on air
     */
    @Override
    public boolean isFavorable() {
        return (potaCount + sotaCount) >= 5;
    }

    /**
     * Calculate score based on total activation count and recency.
     *
     * <p>Scoring algorithm:
     * <ul>
     *   <li>Base: totalCount * 3 points</li>
     *   <li>Recency bonus: +10 if any activation in last 5 minutes</li>
     *   <li>Maximum: 100 points</li>
     * </ul>
     *
     * <p>Examples:
     * <ul>
     *   <li>0 activations: 0 points (cool)</li>
     *   <li>3 activations: 9 points (cool)</li>
     *   <li>5 activations (recent): 25 points (neutral)</li>
     *   <li>10 activations (recent): 40 points (warm)</li>
     *   <li>20 activations (recent): 70 points (hot)</li>
     *   <li>34+ activations: 100 points (hot)</li>
     * </ul>
     *
     * @return score from 0-100 based on activity level
     */
    @Override
    public int getScore() {
        int totalCount = potaCount + sotaCount;
        int baseScore = totalCount * 3;

        // Check if any activation is very recent (last 5 minutes)
        boolean hasRecentActivation = activations != null && activations.stream()
                .anyMatch(a -> a.spottedAt() != null &&
                        java.time.Duration.between(a.spottedAt(), Instant.now()).toMinutes() <= 5);

        int recencyBonus = hasRecentActivation ? 10 : 0;

        return Math.min(100, baseScore + recencyBonus);
    }
}
