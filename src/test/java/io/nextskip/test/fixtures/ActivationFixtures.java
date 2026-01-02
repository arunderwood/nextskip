package io.nextskip.test.fixtures;

import io.nextskip.activations.model.Activation;
import io.nextskip.activations.model.ActivationLocation;
import io.nextskip.activations.model.ActivationType;
import io.nextskip.activations.model.Park;
import io.nextskip.activations.model.Summit;
import io.nextskip.test.TestConstants;
import java.time.Instant;

/**
 * Test fixtures for activation-related domain objects.
 *
 * <p>Provides factory methods and builders for creating test instances of
 * {@link Activation}, {@link Park}, and {@link Summit}.
 */
public final class ActivationFixtures {

    private ActivationFixtures() {
        // Prevent instantiation
    }

    // ==========================================================================
    // Park Factory Methods
    // ==========================================================================

    /**
     * Creates a default Park with standard test values.
     *
     * @return a Park with default test data
     */
    public static Park defaultPark() {
        return new Park(
                TestConstants.DEFAULT_PARK_REF,
                TestConstants.DEFAULT_PARK_NAME,
                TestConstants.DEFAULT_PARK_STATE,
                TestConstants.DEFAULT_PARK_COUNTRY,
                TestConstants.DEFAULT_GRID,
                TestConstants.DEFAULT_LATITUDE,
                TestConstants.DEFAULT_LONGITUDE
        );
    }

    /**
     * Creates a Park builder for customization.
     *
     * @return a new ParkBuilder
     */
    public static ParkBuilder park() {
        return new ParkBuilder();
    }

    // ==========================================================================
    // Summit Factory Methods
    // ==========================================================================

    /**
     * Creates a default Summit with standard test values.
     *
     * @return a Summit with default test data
     */
    public static Summit defaultSummit() {
        return new Summit(
                TestConstants.DEFAULT_SUMMIT_REF,
                TestConstants.DEFAULT_SUMMIT_NAME,
                TestConstants.DEFAULT_SUMMIT_REGION,
                TestConstants.DEFAULT_SUMMIT_ASSOCIATION
        );
    }

    /**
     * Creates a Summit builder for customization.
     *
     * @return a new SummitBuilder
     */
    public static SummitBuilder summit() {
        return new SummitBuilder();
    }

    // ==========================================================================
    // Activation Factory Methods
    // ==========================================================================

    /**
     * Creates a default POTA activation with standard test values.
     *
     * @return a POTA Activation with default test data
     */
    public static Activation defaultPotaActivation() {
        return pota().build();
    }

    /**
     * Creates a default SOTA activation with standard test values.
     *
     * @return a SOTA Activation with default test data
     */
    public static Activation defaultSotaActivation() {
        return sota().build();
    }

    /**
     * Creates a POTA activation builder.
     *
     * @return a new ActivationBuilder configured for POTA
     */
    public static ActivationBuilder pota() {
        return new ActivationBuilder()
                .type(ActivationType.POTA)
                .location(defaultPark());
    }

    /**
     * Creates a SOTA activation builder.
     *
     * @return a new ActivationBuilder configured for SOTA
     */
    public static ActivationBuilder sota() {
        return new ActivationBuilder()
                .type(ActivationType.SOTA)
                .location(defaultSummit());
    }

    // ==========================================================================
    // Park Builder
    // ==========================================================================

    /**
     * Builder for creating customized Park instances.
     */
    public static class ParkBuilder {
        private String reference = TestConstants.DEFAULT_PARK_REF;
        private String name = TestConstants.DEFAULT_PARK_NAME;
        private String regionCode = TestConstants.DEFAULT_PARK_STATE;
        private String countryCode = TestConstants.DEFAULT_PARK_COUNTRY;
        private String grid = TestConstants.DEFAULT_GRID;
        private Double latitude = TestConstants.DEFAULT_LATITUDE;
        private Double longitude = TestConstants.DEFAULT_LONGITUDE;

        public ParkBuilder reference(String reference) {
            this.reference = reference;
            return this;
        }

        public ParkBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ParkBuilder regionCode(String regionCode) {
            this.regionCode = regionCode;
            return this;
        }

        public ParkBuilder countryCode(String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        public ParkBuilder grid(String grid) {
            this.grid = grid;
            return this;
        }

        public ParkBuilder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public ParkBuilder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Park build() {
            return new Park(reference, name, regionCode, countryCode, grid, latitude, longitude);
        }
    }

    // ==========================================================================
    // Summit Builder
    // ==========================================================================

    /**
     * Builder for creating customized Summit instances.
     */
    public static class SummitBuilder {
        private String reference = TestConstants.DEFAULT_SUMMIT_REF;
        private String name = TestConstants.DEFAULT_SUMMIT_NAME;
        private String regionCode = TestConstants.DEFAULT_SUMMIT_REGION;
        private String associationCode = TestConstants.DEFAULT_SUMMIT_ASSOCIATION;

        public SummitBuilder reference(String reference) {
            this.reference = reference;
            return this;
        }

        public SummitBuilder name(String name) {
            this.name = name;
            return this;
        }

        public SummitBuilder regionCode(String regionCode) {
            this.regionCode = regionCode;
            return this;
        }

        public SummitBuilder associationCode(String associationCode) {
            this.associationCode = associationCode;
            return this;
        }

        public Summit build() {
            return new Summit(reference, name, regionCode, associationCode);
        }
    }

    // ==========================================================================
    // Activation Builder
    // ==========================================================================

    /**
     * Builder for creating customized Activation instances.
     */
    public static class ActivationBuilder {
        private String spotId = "test-spot-1";
        private String activatorCallsign = TestConstants.DEFAULT_CALLSIGN;
        private ActivationType type = ActivationType.POTA;
        private Double frequency = TestConstants.DEFAULT_FREQUENCY;
        private String mode = TestConstants.DEFAULT_MODE;
        private Instant spottedAt = Instant.now();
        private Instant lastSeenAt = Instant.now();
        private Integer qsoCount = TestConstants.DEFAULT_QSO_COUNT;
        private String source = TestConstants.DEFAULT_SOURCE;
        private ActivationLocation location = defaultPark();

        public ActivationBuilder spotId(String spotId) {
            this.spotId = spotId;
            return this;
        }

        public ActivationBuilder activatorCallsign(String activatorCallsign) {
            this.activatorCallsign = activatorCallsign;
            return this;
        }

        public ActivationBuilder type(ActivationType type) {
            this.type = type;
            return this;
        }

        public ActivationBuilder frequency(Double frequency) {
            this.frequency = frequency;
            return this;
        }

        public ActivationBuilder mode(String mode) {
            this.mode = mode;
            return this;
        }

        public ActivationBuilder spottedAt(Instant spottedAt) {
            this.spottedAt = spottedAt;
            return this;
        }

        public ActivationBuilder spottedNow() {
            this.spottedAt = Instant.now();
            return this;
        }

        public ActivationBuilder spottedMinutesAgo(long minutes) {
            this.spottedAt = Instant.now().minusSeconds(minutes * 60);
            return this;
        }

        public ActivationBuilder spottedHoursAgo(long hours) {
            this.spottedAt = Instant.now().minusSeconds(hours * 3600);
            return this;
        }

        public ActivationBuilder lastSeenAt(Instant lastSeenAt) {
            this.lastSeenAt = lastSeenAt;
            return this;
        }

        public ActivationBuilder seenNow() {
            this.lastSeenAt = Instant.now();
            return this;
        }

        public ActivationBuilder seenMinutesAgo(long minutes) {
            this.lastSeenAt = Instant.now().minusSeconds(minutes * 60);
            return this;
        }

        public ActivationBuilder seenHoursAgo(long hours) {
            this.lastSeenAt = Instant.now().minusSeconds(hours * 3600);
            return this;
        }

        public ActivationBuilder qsoCount(Integer qsoCount) {
            this.qsoCount = qsoCount;
            return this;
        }

        public ActivationBuilder source(String source) {
            this.source = source;
            return this;
        }

        public ActivationBuilder location(ActivationLocation location) {
            this.location = location;
            return this;
        }

        public Activation build() {
            return new Activation(
                    spotId,
                    activatorCallsign,
                    type,
                    frequency,
                    mode,
                    spottedAt,
                    lastSeenAt,
                    qsoCount,
                    source,
                    location
            );
        }
    }
}
