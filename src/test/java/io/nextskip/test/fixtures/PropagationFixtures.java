package io.nextskip.test.fixtures;

import io.nextskip.common.model.FrequencyBand;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.BandConditionRating;
import io.nextskip.propagation.model.SolarIndices;
import io.nextskip.test.TestConstants;
import java.time.Instant;

/**
 * Test fixtures for propagation-related domain objects.
 *
 * <p>Provides factory methods and builders for creating test instances of
 * {@link SolarIndices} and {@link BandCondition}.
 */
public final class PropagationFixtures {

    private PropagationFixtures() {
        // Prevent instantiation
    }

    // ==========================================================================
    // SolarIndices Factory Methods
    // ==========================================================================

    /**
     * Creates default SolarIndices with good propagation conditions.
     *
     * <p>Represents typical good HF conditions:
     * <ul>
     *   <li>SFI = 100 (moderate to good)</li>
     *   <li>K-index = 2 (quiet geomagnetic)</li>
     *   <li>A-index = 10 (quiet)</li>
     *   <li>Sunspot = 50 (moderate activity)</li>
     * </ul>
     *
     * @return SolarIndices with good conditions
     */
    public static SolarIndices defaultSolarIndices() {
        return solarIndices().build();
    }

    /**
     * Creates SolarIndices representing excellent propagation conditions.
     *
     * @return SolarIndices with excellent conditions (high SFI, low K)
     */
    public static SolarIndices excellentConditions() {
        return solarIndices()
                .solarFluxIndex(180.0)
                .kIndex(1)
                .aIndex(5)
                .sunspotNumber(100)
                .build();
    }

    /**
     * Creates SolarIndices representing poor propagation conditions.
     *
     * @return SolarIndices with poor conditions (low SFI, high K)
     */
    public static SolarIndices poorConditions() {
        return solarIndices()
                .solarFluxIndex(65.0)
                .kIndex(6)
                .aIndex(30)
                .sunspotNumber(10)
                .build();
    }

    /**
     * Creates a SolarIndices builder for customization.
     *
     * @return a new SolarIndicesBuilder with default values
     */
    public static SolarIndicesBuilder solarIndices() {
        return new SolarIndicesBuilder();
    }

    // ==========================================================================
    // BandCondition Factory Methods
    // ==========================================================================

    /**
     * Creates a good BandCondition for the specified band.
     *
     * @param band the frequency band
     * @return BandCondition with GOOD rating and high confidence
     */
    public static BandCondition goodBandCondition(FrequencyBand band) {
        return new BandCondition(band, BandConditionRating.GOOD, 1.0, null);
    }

    /**
     * Creates a fair BandCondition for the specified band.
     *
     * @param band the frequency band
     * @return BandCondition with FAIR rating and high confidence
     */
    public static BandCondition fairBandCondition(FrequencyBand band) {
        return new BandCondition(band, BandConditionRating.FAIR, 1.0, null);
    }

    /**
     * Creates a poor BandCondition for the specified band.
     *
     * @param band the frequency band
     * @return BandCondition with POOR rating and high confidence
     */
    public static BandCondition poorBandCondition(FrequencyBand band) {
        return new BandCondition(band, BandConditionRating.POOR, 1.0, null);
    }

    /**
     * Creates an unknown BandCondition for the specified band.
     *
     * @param band the frequency band
     * @return BandCondition with UNKNOWN rating
     */
    public static BandCondition unknownBandCondition(FrequencyBand band) {
        return new BandCondition(band, BandConditionRating.UNKNOWN, 0.0, null);
    }

    /**
     * Creates a good BandCondition for 20m band (common default).
     *
     * @return BandCondition for 20m with GOOD rating
     */
    public static BandCondition defaultBandCondition() {
        return goodBandCondition(FrequencyBand.BAND_20M);
    }

    /**
     * Creates a BandCondition builder for customization.
     *
     * @return a new BandConditionBuilder with default values
     */
    public static BandConditionBuilder bandCondition() {
        return new BandConditionBuilder();
    }

    // ==========================================================================
    // SolarIndices Builder
    // ==========================================================================

    /**
     * Builder for creating customized SolarIndices instances.
     */
    public static class SolarIndicesBuilder {
        private double solarFluxIndex = TestConstants.DEFAULT_SFI;
        private int aIndex = TestConstants.DEFAULT_A_INDEX;
        private int kIndex = TestConstants.DEFAULT_K_INDEX;
        private int sunspotNumber = TestConstants.DEFAULT_SUNSPOT_NUMBER;
        private Instant timestamp = Instant.now();
        private String source = TestConstants.DEFAULT_SOURCE;

        public SolarIndicesBuilder solarFluxIndex(double solarFluxIndex) {
            this.solarFluxIndex = solarFluxIndex;
            return this;
        }

        public SolarIndicesBuilder aIndex(int aIndex) {
            this.aIndex = aIndex;
            return this;
        }

        public SolarIndicesBuilder kIndex(int kIndex) {
            this.kIndex = kIndex;
            return this;
        }

        public SolarIndicesBuilder sunspotNumber(int sunspotNumber) {
            this.sunspotNumber = sunspotNumber;
            return this;
        }

        public SolarIndicesBuilder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public SolarIndicesBuilder source(String source) {
            this.source = source;
            return this;
        }

        public SolarIndices build() {
            return new SolarIndices(solarFluxIndex, aIndex, kIndex, sunspotNumber, timestamp, source);
        }
    }

    // ==========================================================================
    // BandCondition Builder
    // ==========================================================================

    /**
     * Builder for creating customized BandCondition instances.
     */
    public static class BandConditionBuilder {
        private FrequencyBand band = FrequencyBand.BAND_20M;
        private BandConditionRating rating = BandConditionRating.GOOD;
        private double confidence = 1.0;
        private String notes;

        public BandConditionBuilder band(FrequencyBand band) {
            this.band = band;
            return this;
        }

        public BandConditionBuilder rating(BandConditionRating rating) {
            this.rating = rating;
            return this;
        }

        public BandConditionBuilder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }

        public BandConditionBuilder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public BandCondition build() {
            return new BandCondition(band, rating, confidence, notes);
        }
    }
}
