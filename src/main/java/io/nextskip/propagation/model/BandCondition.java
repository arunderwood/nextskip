package io.nextskip.propagation.model;

import io.nextskip.common.model.FrequencyBand;

/**
 * Propagation condition for a specific amateur radio band.
 *
 * @param band       The frequency band
 * @param rating     Condition rating (GOOD/FAIR/POOR/UNKNOWN)
 * @param confidence Confidence level 0.0-1.0 (1.0 = highest confidence)
 * @param notes      Optional notes about the condition
 */
public record BandCondition(
        FrequencyBand band,
        BandConditionRating rating,
        double confidence,
        String notes
) {
    public BandCondition {
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }
    }

    /**
     * Create a BandCondition with default confidence.
     */
    public BandCondition(FrequencyBand band, BandConditionRating rating) {
        this(band, rating, 1.0, null);
    }

    /**
     * Create a BandCondition with confidence but no notes.
     */
    public BandCondition(FrequencyBand band, BandConditionRating rating, double confidence) {
        this(band, rating, confidence, null);
    }

    /**
     * Check if this band is currently favorable for propagation.
     */
    public boolean isFavorable() {
        return rating == BandConditionRating.GOOD && confidence > 0.5;
    }

    /**
     * Get a score for this band condition (0-100).
     * Used for sorting or prioritizing bands.
     */
    public int getScore() {
        return switch (rating) {
            case GOOD -> (int) (100 * confidence);
            case FAIR -> (int) (60 * confidence);
            case POOR -> (int) (20 * confidence);
            case UNKNOWN -> 0;
        };
    }
}
