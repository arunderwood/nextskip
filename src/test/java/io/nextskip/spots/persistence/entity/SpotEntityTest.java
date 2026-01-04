package io.nextskip.spots.persistence.entity;

import io.nextskip.spots.model.Spot;
import io.nextskip.test.fixtures.SpotFixtures;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static io.nextskip.test.TestConstants.EU_GRID;
import static io.nextskip.test.TestConstants.FT8_20M_FREQUENCY_HZ;
import static io.nextskip.test.TestConstants.NA_GRID;
import static io.nextskip.test.TestConstants.PSKREPORTER_SOURCE;
import static io.nextskip.test.TestConstants.TRANSATLANTIC_DISTANCE_KM;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SpotEntity}.
 *
 * <p>Tests entity construction, domain conversion, and Clock injection.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Test data uses repeated band/mode/callsign values
class SpotEntityTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2023-06-15T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneId.of("UTC"));

    // ===========================================
    // fromDomain tests
    // ===========================================

    @Test
    void testFromDomain_AllFields_CopiesCorrectly() {
        Instant spottedAt = Instant.parse("2023-06-15T10:30:00Z");
        Spot spot = SpotFixtures.spot()
                .source(PSKREPORTER_SOURCE)
                .band("20m")
                .mode("FT8")
                .frequencyHz(FT8_20M_FREQUENCY_HZ)
                .snr(-10)
                .spottedAt(spottedAt)
                .spotterCall("W1AW")
                .spotterGrid(NA_GRID)
                .spotterContinent("NA")
                .spottedCall("G3ABC")
                .spottedGrid(EU_GRID)
                .spottedContinent("EU")
                .distanceKm(TRANSATLANTIC_DISTANCE_KM)
                .build();

        SpotEntity entity = SpotEntity.fromDomain(spot, FIXED_CLOCK);

        assertThat(entity.getSource()).isEqualTo(PSKREPORTER_SOURCE);
        assertThat(entity.getBand()).isEqualTo("20m");
        assertThat(entity.getMode()).isEqualTo("FT8");
        assertThat(entity.getFrequencyHz()).isEqualTo(FT8_20M_FREQUENCY_HZ);
        assertThat(entity.getSnr()).isEqualTo(-10);
        assertThat(entity.getSpottedAt()).isEqualTo(spottedAt);
        assertThat(entity.getSpotterCall()).isEqualTo("W1AW");
        assertThat(entity.getSpotterGrid()).isEqualTo(NA_GRID);
        assertThat(entity.getSpotterContinent()).isEqualTo("NA");
        assertThat(entity.getSpottedCall()).isEqualTo("G3ABC");
        assertThat(entity.getSpottedGrid()).isEqualTo(EU_GRID);
        assertThat(entity.getSpottedContinent()).isEqualTo("EU");
        assertThat(entity.getDistanceKm()).isEqualTo(TRANSATLANTIC_DISTANCE_KM);
    }

    @Test
    void testFromDomain_NullOptionalFields_PreservesNulls() {
        Spot spot = SpotFixtures.spot()
                .frequencyHz(null)
                .snr(null)
                .spotterCall(null)
                .spotterGrid(null)
                .spotterContinent(null)
                .spottedCall(null)
                .spottedGrid(null)
                .spottedContinent(null)
                .distanceKm(null)
                .build();

        SpotEntity entity = SpotEntity.fromDomain(spot, FIXED_CLOCK);

        assertThat(entity.getFrequencyHz()).isNull();
        assertThat(entity.getSnr()).isNull();
        assertThat(entity.getSpotterCall()).isNull();
        assertThat(entity.getSpotterGrid()).isNull();
        assertThat(entity.getSpotterContinent()).isNull();
        assertThat(entity.getSpottedCall()).isNull();
        assertThat(entity.getSpottedGrid()).isNull();
        assertThat(entity.getSpottedContinent()).isNull();
        assertThat(entity.getDistanceKm()).isNull();
    }

    @Test
    void testFromDomain_WithClock_SetsCreatedAtFromClock() {
        Spot spot = SpotFixtures.defaultSpot();

        SpotEntity entity = SpotEntity.fromDomain(spot, FIXED_CLOCK);

        assertThat(entity.getCreatedAt()).isEqualTo(FIXED_INSTANT);
    }

    @Test
    void testFromDomain_WithoutClock_SetsCreatedAtNow() {
        Spot spot = SpotFixtures.defaultSpot();
        Instant before = Instant.now();

        SpotEntity entity = SpotEntity.fromDomain(spot);

        Instant after = Instant.now();
        assertThat(entity.getCreatedAt())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }

    @Test
    void testFromDomain_IdIsNull() {
        Spot spot = SpotFixtures.defaultSpot();

        SpotEntity entity = SpotEntity.fromDomain(spot, FIXED_CLOCK);

        assertThat(entity.getId()).isNull();
    }

    // ===========================================
    // toDomain tests
    // ===========================================

    @Test
    void testToDomain_AllFields_ConvertedCorrectly() {
        Spot original = SpotFixtures.defaultSpot();
        SpotEntity entity = SpotEntity.fromDomain(original, FIXED_CLOCK);

        Spot converted = entity.toDomain();

        assertThat(converted.source()).isEqualTo(original.source());
        assertThat(converted.band()).isEqualTo(original.band());
        assertThat(converted.mode()).isEqualTo(original.mode());
        assertThat(converted.frequencyHz()).isEqualTo(original.frequencyHz());
        assertThat(converted.snr()).isEqualTo(original.snr());
        assertThat(converted.spottedAt()).isEqualTo(original.spottedAt());
        assertThat(converted.spotterCall()).isEqualTo(original.spotterCall());
        assertThat(converted.spotterGrid()).isEqualTo(original.spotterGrid());
        assertThat(converted.spotterContinent()).isEqualTo(original.spotterContinent());
        assertThat(converted.spottedCall()).isEqualTo(original.spottedCall());
        assertThat(converted.spottedGrid()).isEqualTo(original.spottedGrid());
        assertThat(converted.spottedContinent()).isEqualTo(original.spottedContinent());
        assertThat(converted.distanceKm()).isEqualTo(original.distanceKm());
    }

    @Test
    void testToDomain_NullOptionalFields_PreservesNulls() {
        Spot original = SpotFixtures.spot()
                .frequencyHz(null)
                .snr(null)
                .spotterCall(null)
                .spotterGrid(null)
                .spotterContinent(null)
                .spottedCall(null)
                .spottedGrid(null)
                .spottedContinent(null)
                .distanceKm(null)
                .build();
        SpotEntity entity = SpotEntity.fromDomain(original, FIXED_CLOCK);

        Spot converted = entity.toDomain();

        assertThat(converted.frequencyHz()).isNull();
        assertThat(converted.snr()).isNull();
        assertThat(converted.spotterCall()).isNull();
        assertThat(converted.spotterGrid()).isNull();
        assertThat(converted.spotterContinent()).isNull();
        assertThat(converted.spottedCall()).isNull();
        assertThat(converted.spottedGrid()).isNull();
        assertThat(converted.spottedContinent()).isNull();
        assertThat(converted.distanceKm()).isNull();
    }

    @Test
    void testToDomain_DoesNotIncludeEntityFields() {
        // createdAt and id are entity-specific, not part of domain model
        Spot original = SpotFixtures.defaultSpot();
        SpotEntity entity = SpotEntity.fromDomain(original, FIXED_CLOCK);
        entity.setId(123L);

        Spot converted = entity.toDomain();

        // Verify the domain Spot doesn't expose entity-specific fields
        // (Spot record has no id or createdAt fields)
        assertThat(converted).isEqualTo(original);
    }

    // ===========================================
    // Round-trip tests
    // ===========================================

    @Test
    void testRoundTrip_SpotThroughEntity_PreservesData() {
        Spot original = SpotFixtures.spot()
                .source(PSKREPORTER_SOURCE)
                .band("40m")
                .mode("CW")
                .frequencyHz(7025000L)
                .snr(-15)
                .spotterCall("W6ABC")
                .spotterGrid("CM97")
                .spotterContinent("NA")
                .spottedCall("JA1XYZ")
                .spottedGrid("PM95")
                .spottedContinent("AS")
                .distanceKm(8500)
                .build();

        SpotEntity entity = SpotEntity.fromDomain(original, FIXED_CLOCK);
        Spot roundTripped = entity.toDomain();

        assertThat(roundTripped).isEqualTo(original);
    }

    @Test
    void testRoundTrip_UnenrichedSpot_PreservesNulls() {
        Spot original = SpotFixtures.spot().unenriched().build();

        SpotEntity entity = SpotEntity.fromDomain(original, FIXED_CLOCK);
        Spot roundTripped = entity.toDomain();

        assertThat(roundTripped.spotterContinent()).isNull();
        assertThat(roundTripped.spottedContinent()).isNull();
        assertThat(roundTripped.distanceKm()).isNull();
    }

    // ===========================================
    // Constructor tests
    // ===========================================

    @Test
    void testConstructor_AllArgs_SetsAllFields() {
        Instant spottedAt = Instant.parse("2023-06-15T10:00:00Z");
        Instant createdAt = Instant.parse("2023-06-15T10:01:00Z");

        SpotEntity entity = new SpotEntity(
                PSKREPORTER_SOURCE,
                "20m",
                "FT8",
                FT8_20M_FREQUENCY_HZ,
                -10,
                spottedAt,
                "W1AW",
                NA_GRID,
                "NA",
                "G3ABC",
                EU_GRID,
                "EU",
                TRANSATLANTIC_DISTANCE_KM,
                createdAt
        );

        assertThat(entity.getSource()).isEqualTo(PSKREPORTER_SOURCE);
        assertThat(entity.getBand()).isEqualTo("20m");
        assertThat(entity.getMode()).isEqualTo("FT8");
        assertThat(entity.getFrequencyHz()).isEqualTo(FT8_20M_FREQUENCY_HZ);
        assertThat(entity.getSnr()).isEqualTo(-10);
        assertThat(entity.getSpottedAt()).isEqualTo(spottedAt);
        assertThat(entity.getSpotterCall()).isEqualTo("W1AW");
        assertThat(entity.getSpotterGrid()).isEqualTo(NA_GRID);
        assertThat(entity.getSpotterContinent()).isEqualTo("NA");
        assertThat(entity.getSpottedCall()).isEqualTo("G3ABC");
        assertThat(entity.getSpottedGrid()).isEqualTo(EU_GRID);
        assertThat(entity.getSpottedContinent()).isEqualTo("EU");
        assertThat(entity.getDistanceKm()).isEqualTo(TRANSATLANTIC_DISTANCE_KM);
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getId()).isNull();
    }

    // ===========================================
    // Setters tests
    // ===========================================

    @Test
    void testSetters_ModifyEntity() {
        SpotEntity entity = SpotFixtures.defaultSpotEntity(FIXED_CLOCK);

        entity.setId(999L);
        entity.setSource("TestSource");
        entity.setBand("80m");
        entity.setMode("CW");
        entity.setFrequencyHz(3500000L);
        entity.setSnr(-25);
        entity.setSpotterCall("K3XYZ");
        entity.setSpotterGrid("FM29");
        entity.setSpotterContinent("NA");
        entity.setSpottedCall("DL1ABC");
        entity.setSpottedGrid("JN48");
        entity.setSpottedContinent("EU");
        entity.setDistanceKm(6500);

        assertThat(entity.getId()).isEqualTo(999L);
        assertThat(entity.getSource()).isEqualTo("TestSource");
        assertThat(entity.getBand()).isEqualTo("80m");
        assertThat(entity.getMode()).isEqualTo("CW");
        assertThat(entity.getFrequencyHz()).isEqualTo(3500000L);
        assertThat(entity.getSnr()).isEqualTo(-25);
        assertThat(entity.getSpotterCall()).isEqualTo("K3XYZ");
        assertThat(entity.getSpotterGrid()).isEqualTo("FM29");
        assertThat(entity.getSpotterContinent()).isEqualTo("NA");
        assertThat(entity.getSpottedCall()).isEqualTo("DL1ABC");
        assertThat(entity.getSpottedGrid()).isEqualTo("JN48");
        assertThat(entity.getSpottedContinent()).isEqualTo("EU");
        assertThat(entity.getDistanceKm()).isEqualTo(6500);
    }

    @Test
    void testSetCreatedAt_ModifiesCreatedAt() {
        SpotEntity entity = SpotFixtures.defaultSpotEntity(FIXED_CLOCK);
        Instant newCreatedAt = Instant.parse("2023-12-25T00:00:00Z");

        entity.setCreatedAt(newCreatedAt);

        assertThat(entity.getCreatedAt()).isEqualTo(newCreatedAt);
    }

    @Test
    void testSetSpottedAt_ModifiesSpottedAt() {
        SpotEntity entity = SpotFixtures.defaultSpotEntity(FIXED_CLOCK);
        Instant newSpottedAt = Instant.parse("2023-07-04T12:00:00Z");

        entity.setSpottedAt(newSpottedAt);

        assertThat(entity.getSpottedAt()).isEqualTo(newSpottedAt);
    }

    // ===========================================
    // Fixture tests
    // ===========================================

    @Test
    void testSpotFixtures_DefaultSpotEntity_HasExpectedValues() {
        SpotEntity entity = SpotFixtures.defaultSpotEntity(FIXED_CLOCK);

        assertThat(entity.getSource()).isEqualTo(PSKREPORTER_SOURCE);
        assertThat(entity.getBand()).isEqualTo("20m");
        assertThat(entity.getMode()).isEqualTo("FT8");
        assertThat(entity.getCreatedAt()).isEqualTo(FIXED_INSTANT);
    }

    @Test
    void testSpotFixtures_SpotEntity_WithCustomClock() {
        Spot spot = SpotFixtures.spot().band("10m").build();

        SpotEntity entity = SpotFixtures.spotEntity(spot, FIXED_CLOCK);

        assertThat(entity.getBand()).isEqualTo("10m");
        assertThat(entity.getCreatedAt()).isEqualTo(FIXED_INSTANT);
    }
}
