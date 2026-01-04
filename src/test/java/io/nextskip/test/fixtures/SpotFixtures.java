package io.nextskip.test.fixtures;

import io.nextskip.spots.model.Spot;
import io.nextskip.spots.persistence.entity.SpotEntity;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Test fixtures for Spot domain model and entity.
 *
 * <p>Provides factory methods and builders for creating test data
 * following the patterns established in {@link ActivationFixtures}.
 *
 * <p>Usage:
 * <pre>{@code
 * // Quick default spot
 * Spot spot = SpotFixtures.defaultSpot();
 *
 * // Customized spot via builder
 * Spot spot = SpotFixtures.spot()
 *     .band("40m")
 *     .mode("CW")
 *     .distanceKm(8000)
 *     .build();
 * }</pre>
 */
public final class SpotFixtures {

    private SpotFixtures() {
        // Utility class
    }

    /**
     * Creates a default spot with typical FT8 values.
     *
     * @return a spot with all fields populated
     */
    public static Spot defaultSpot() {
        return spot().build();
    }

    /**
     * Creates a spot builder pre-configured with default values.
     *
     * @return a new builder
     */
    public static SpotBuilder spot() {
        return new SpotBuilder();
    }

    /**
     * Creates a default SpotEntity with typical values.
     *
     * @return a spot entity ready for persistence testing
     */
    public static SpotEntity defaultSpotEntity() {
        return SpotEntity.fromDomain(defaultSpot());
    }

    /**
     * Creates a default SpotEntity with a custom clock.
     *
     * @param clock the clock to use for createdAt timestamp
     * @return a spot entity ready for persistence testing
     */
    public static SpotEntity defaultSpotEntity(Clock clock) {
        return SpotEntity.fromDomain(defaultSpot(), clock);
    }

    /**
     * Creates a SpotEntity from a Spot with a custom clock.
     *
     * @param spot  the domain model
     * @param clock the clock to use for createdAt timestamp
     * @return a spot entity ready for persistence testing
     */
    public static SpotEntity spotEntity(Spot spot, Clock clock) {
        return SpotEntity.fromDomain(spot, clock);
    }

    /**
     * Builder for creating customized Spot instances.
     */
    @SuppressWarnings("PMD.TooManyMethods") // Builder pattern requires method per field
    public static class SpotBuilder {

        private String source = "PSKReporter";
        private String band = "20m";
        private String mode = "FT8";
        private Long frequencyHz = 14074000L;
        private Integer snr = -10;
        private Instant spottedAt = Instant.now();
        private String spotterCall = "W1AW";
        private String spotterGrid = "FN31";
        private String spotterContinent = "NA";
        private String spottedCall = "G3ABC";
        private String spottedGrid = "JO01";
        private String spottedContinent = "EU";
        private Integer distanceKm = 5500;

        public SpotBuilder source(String source) {
            this.source = source;
            return this;
        }

        public SpotBuilder band(String band) {
            this.band = band;
            return this;
        }

        public SpotBuilder mode(String mode) {
            this.mode = mode;
            return this;
        }

        public SpotBuilder frequencyHz(Long frequencyHz) {
            this.frequencyHz = frequencyHz;
            return this;
        }

        public SpotBuilder snr(Integer snr) {
            this.snr = snr;
            return this;
        }

        public SpotBuilder spottedAt(Instant spottedAt) {
            this.spottedAt = spottedAt;
            return this;
        }

        /**
         * Sets spottedAt to current time.
         */
        public SpotBuilder spottedNow() {
            this.spottedAt = Instant.now();
            return this;
        }

        /**
         * Sets spottedAt to the specified minutes ago.
         */
        public SpotBuilder spottedMinutesAgo(int minutes) {
            this.spottedAt = Instant.now().minus(minutes, ChronoUnit.MINUTES);
            return this;
        }

        /**
         * Sets spottedAt to the specified hours ago.
         */
        public SpotBuilder spottedHoursAgo(int hours) {
            this.spottedAt = Instant.now().minus(hours, ChronoUnit.HOURS);
            return this;
        }

        public SpotBuilder spotterCall(String spotterCall) {
            this.spotterCall = spotterCall;
            return this;
        }

        public SpotBuilder spotterGrid(String spotterGrid) {
            this.spotterGrid = spotterGrid;
            return this;
        }

        public SpotBuilder spotterContinent(String spotterContinent) {
            this.spotterContinent = spotterContinent;
            return this;
        }

        public SpotBuilder spottedCall(String spottedCall) {
            this.spottedCall = spottedCall;
            return this;
        }

        public SpotBuilder spottedGrid(String spottedGrid) {
            this.spottedGrid = spottedGrid;
            return this;
        }

        public SpotBuilder spottedContinent(String spottedContinent) {
            this.spottedContinent = spottedContinent;
            return this;
        }

        public SpotBuilder distanceKm(Integer distanceKm) {
            this.distanceKm = distanceKm;
            return this;
        }

        /**
         * Clears enriched fields (continents and distance).
         * Useful for testing enrichment logic.
         */
        public SpotBuilder unenriched() {
            this.spotterContinent = null;
            this.spottedContinent = null;
            this.distanceKm = null;
            return this;
        }

        /**
         * Configures a trans-Atlantic path (NA to EU).
         */
        public SpotBuilder transAtlantic() {
            this.spotterCall = "W1AW";
            this.spotterGrid = "FN31";
            this.spotterContinent = "NA";
            this.spottedCall = "G3ABC";
            this.spottedGrid = "JO01";
            this.spottedContinent = "EU";
            this.distanceKm = 5500;
            return this;
        }

        /**
         * Configures a trans-Pacific path (NA to AS).
         */
        public SpotBuilder transPacific() {
            this.spotterCall = "W6ABC";
            this.spotterGrid = "CM97";
            this.spotterContinent = "NA";
            this.spottedCall = "JA1ABC";
            this.spottedGrid = "PM95";
            this.spottedContinent = "AS";
            this.distanceKm = 8500;
            return this;
        }

        /**
         * Configures a local path (same continent).
         */
        public SpotBuilder local() {
            this.spotterCall = "W1AW";
            this.spotterGrid = "FN31";
            this.spotterContinent = "NA";
            this.spottedCall = "W6XYZ";
            this.spottedGrid = "CM97";
            this.spottedContinent = "NA";
            this.distanceKm = 4000;
            return this;
        }

        /**
         * Builds the Spot instance.
         *
         * @return a new Spot
         */
        public Spot build() {
            return new Spot(
                    source,
                    band,
                    mode,
                    frequencyHz,
                    snr,
                    spottedAt,
                    spotterCall,
                    spotterGrid,
                    spotterContinent,
                    spottedCall,
                    spottedGrid,
                    spottedContinent,
                    distanceKm
            );
        }
    }
}
