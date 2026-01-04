package io.nextskip.spots.model;

import io.nextskip.test.fixtures.SpotFixtures;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static io.nextskip.test.TestConstants.EU_GRID;
import static io.nextskip.test.TestConstants.FT8_20M_FREQUENCY_HZ;
import static io.nextskip.test.TestConstants.NA_GRID;
import static io.nextskip.test.TestConstants.PSKREPORTER_SOURCE;
import static io.nextskip.test.TestConstants.TRANSATLANTIC_DISTANCE_KM;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Spot} record.
 *
 * <p>Tests record construction, immutability, and wither methods.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Test data uses repeated band/mode/callsign values
class SpotTest {

    // ===========================================
    // Record construction tests
    // ===========================================

    @Test
    void testSpot_FullConstruction_AllFieldsSet() {
        Instant spottedAt = Instant.now();

        Spot spot = new Spot(
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
                TRANSATLANTIC_DISTANCE_KM
        );

        assertThat(spot.source()).isEqualTo(PSKREPORTER_SOURCE);
        assertThat(spot.band()).isEqualTo("20m");
        assertThat(spot.mode()).isEqualTo("FT8");
        assertThat(spot.frequencyHz()).isEqualTo(FT8_20M_FREQUENCY_HZ);
        assertThat(spot.snr()).isEqualTo(-10);
        assertThat(spot.spottedAt()).isEqualTo(spottedAt);
        assertThat(spot.spotterCall()).isEqualTo("W1AW");
        assertThat(spot.spotterGrid()).isEqualTo(NA_GRID);
        assertThat(spot.spotterContinent()).isEqualTo("NA");
        assertThat(spot.spottedCall()).isEqualTo("G3ABC");
        assertThat(spot.spottedGrid()).isEqualTo(EU_GRID);
        assertThat(spot.spottedContinent()).isEqualTo("EU");
        assertThat(spot.distanceKm()).isEqualTo(TRANSATLANTIC_DISTANCE_KM);
    }

    @Test
    void testSpot_NullOptionalFields_AcceptsNulls() {
        Instant spottedAt = Instant.now();

        Spot spot = new Spot(
                PSKREPORTER_SOURCE,
                "20m",
                "FT8",
                null,  // frequencyHz
                null,  // snr
                spottedAt,
                null,  // spotterCall
                null,  // spotterGrid
                null,  // spotterContinent
                null,  // spottedCall
                null,  // spottedGrid
                null,  // spottedContinent
                null   // distanceKm
        );

        assertThat(spot.frequencyHz()).isNull();
        assertThat(spot.snr()).isNull();
        assertThat(spot.spotterCall()).isNull();
        assertThat(spot.spotterGrid()).isNull();
        assertThat(spot.spotterContinent()).isNull();
        assertThat(spot.spottedCall()).isNull();
        assertThat(spot.spottedGrid()).isNull();
        assertThat(spot.spottedContinent()).isNull();
        assertThat(spot.distanceKm()).isNull();
    }

    // ===========================================
    // withDistance tests
    // ===========================================

    @Test
    void testWithDistance_SetsDistance_ReturnsNewSpot() {
        Spot original = SpotFixtures.spot()
                .distanceKm(null)
                .build();

        Spot withDistance = original.withDistance(5500);

        assertThat(withDistance.distanceKm()).isEqualTo(5500);
        assertThat(original.distanceKm()).isNull();  // Original unchanged
    }

    @Test
    void testWithDistance_PreservesOtherFields() {
        Spot original = SpotFixtures.defaultSpot();

        Spot withDistance = original.withDistance(9999);

        assertThat(withDistance.source()).isEqualTo(original.source());
        assertThat(withDistance.band()).isEqualTo(original.band());
        assertThat(withDistance.mode()).isEqualTo(original.mode());
        assertThat(withDistance.frequencyHz()).isEqualTo(original.frequencyHz());
        assertThat(withDistance.snr()).isEqualTo(original.snr());
        assertThat(withDistance.spottedAt()).isEqualTo(original.spottedAt());
        assertThat(withDistance.spotterCall()).isEqualTo(original.spotterCall());
        assertThat(withDistance.spotterGrid()).isEqualTo(original.spotterGrid());
        assertThat(withDistance.spotterContinent()).isEqualTo(original.spotterContinent());
        assertThat(withDistance.spottedCall()).isEqualTo(original.spottedCall());
        assertThat(withDistance.spottedGrid()).isEqualTo(original.spottedGrid());
        assertThat(withDistance.spottedContinent()).isEqualTo(original.spottedContinent());
    }

    @Test
    void testWithDistance_NullValue_SetsToNull() {
        Spot original = SpotFixtures.spot()
                .distanceKm(5000)
                .build();

        Spot withDistance = original.withDistance(null);

        assertThat(withDistance.distanceKm()).isNull();
    }

    @Test
    void testWithDistance_ReturnsNewInstance() {
        Spot original = SpotFixtures.defaultSpot();

        Spot withDistance = original.withDistance(1000);

        assertThat(withDistance).isNotSameAs(original);
    }

    // ===========================================
    // withContinents tests
    // ===========================================

    @Test
    void testWithContinents_SetsContinents_ReturnsNewSpot() {
        Spot original = SpotFixtures.spot()
                .spotterContinent(null)
                .spottedContinent(null)
                .build();

        Spot withContinents = original.withContinents("NA", "EU");

        assertThat(withContinents.spotterContinent()).isEqualTo("NA");
        assertThat(withContinents.spottedContinent()).isEqualTo("EU");
        assertThat(original.spotterContinent()).isNull();  // Original unchanged
        assertThat(original.spottedContinent()).isNull();
    }

    @Test
    void testWithContinents_PreservesOtherFields() {
        Spot original = SpotFixtures.defaultSpot();

        Spot withContinents = original.withContinents("AS", "OC");

        assertThat(withContinents.source()).isEqualTo(original.source());
        assertThat(withContinents.band()).isEqualTo(original.band());
        assertThat(withContinents.mode()).isEqualTo(original.mode());
        assertThat(withContinents.frequencyHz()).isEqualTo(original.frequencyHz());
        assertThat(withContinents.snr()).isEqualTo(original.snr());
        assertThat(withContinents.spottedAt()).isEqualTo(original.spottedAt());
        assertThat(withContinents.spotterCall()).isEqualTo(original.spotterCall());
        assertThat(withContinents.spotterGrid()).isEqualTo(original.spotterGrid());
        assertThat(withContinents.spottedCall()).isEqualTo(original.spottedCall());
        assertThat(withContinents.spottedGrid()).isEqualTo(original.spottedGrid());
        assertThat(withContinents.distanceKm()).isEqualTo(original.distanceKm());
    }

    @Test
    void testWithContinents_NullValues_SetsToNull() {
        Spot original = SpotFixtures.spot()
                .spotterContinent("NA")
                .spottedContinent("EU")
                .build();

        Spot withContinents = original.withContinents(null, null);

        assertThat(withContinents.spotterContinent()).isNull();
        assertThat(withContinents.spottedContinent()).isNull();
    }

    @Test
    void testWithContinents_ReturnsNewInstance() {
        Spot original = SpotFixtures.defaultSpot();

        Spot withContinents = original.withContinents("NA", "EU");

        assertThat(withContinents).isNotSameAs(original);
    }

    // ===========================================
    // Record equality tests
    // ===========================================

    @Test
    void testEquals_SameFields_AreEqual() {
        Instant spottedAt = Instant.parse("2023-01-01T00:00:00Z");

        Spot spot1 = SpotFixtures.spot()
                .spottedAt(spottedAt)
                .build();

        Spot spot2 = SpotFixtures.spot()
                .spottedAt(spottedAt)
                .build();

        assertThat(spot1).isEqualTo(spot2);
        assertThat(spot1.hashCode()).isEqualTo(spot2.hashCode());
    }

    @Test
    void testEquals_DifferentFields_AreNotEqual() {
        Spot spot1 = SpotFixtures.spot()
                .band("20m")
                .build();

        Spot spot2 = SpotFixtures.spot()
                .band("40m")
                .build();

        assertThat(spot1).isNotEqualTo(spot2);
    }

    @Test
    void testEquals_NullComparison_NotEqual() {
        Spot spot = SpotFixtures.defaultSpot();

        assertThat(spot).isNotEqualTo(null);
    }

    // ===========================================
    // toString tests
    // ===========================================

    @Test
    void testToString_ContainsAllFields() {
        Spot spot = SpotFixtures.spot()
                .source(PSKREPORTER_SOURCE)
                .band("20m")
                .mode("FT8")
                .spotterCall("W1AW")
                .spottedCall("G3ABC")
                .build();

        String str = spot.toString();

        assertThat(str).contains(PSKREPORTER_SOURCE);
        assertThat(str).contains("20m");
        assertThat(str).contains("FT8");
        assertThat(str).contains("W1AW");
        assertThat(str).contains("G3ABC");
    }

    // ===========================================
    // Fixture integration tests
    // ===========================================

    @Test
    void testSpotFixtures_DefaultSpot_HasExpectedDefaults() {
        Spot spot = SpotFixtures.defaultSpot();

        assertThat(spot.source()).isEqualTo(PSKREPORTER_SOURCE);
        assertThat(spot.band()).isEqualTo("20m");
        assertThat(spot.mode()).isEqualTo("FT8");
        assertThat(spot.frequencyHz()).isEqualTo(FT8_20M_FREQUENCY_HZ);
    }

    @Test
    void testSpotFixtures_TransAtlantic_HasCorrectPath() {
        Spot spot = SpotFixtures.spot().transAtlantic().build();

        assertThat(spot.spotterContinent()).isEqualTo("NA");
        assertThat(spot.spottedContinent()).isEqualTo("EU");
        assertThat(spot.spotterGrid()).isEqualTo(NA_GRID);
        assertThat(spot.spottedGrid()).isEqualTo(EU_GRID);
    }

    @Test
    void testSpotFixtures_TransPacific_HasCorrectPath() {
        Spot spot = SpotFixtures.spot().transPacific().build();

        assertThat(spot.spotterContinent()).isEqualTo("NA");
        assertThat(spot.spottedContinent()).isEqualTo("AS");
    }

    @Test
    void testSpotFixtures_Local_HasSameContinent() {
        Spot spot = SpotFixtures.spot().local().build();

        assertThat(spot.spotterContinent()).isEqualTo(spot.spottedContinent());
    }

    @Test
    void testSpotFixtures_Unenriched_HasNullEnrichedFields() {
        Spot spot = SpotFixtures.spot().unenriched().build();

        assertThat(spot.spotterContinent()).isNull();
        assertThat(spot.spottedContinent()).isNull();
        assertThat(spot.distanceKm()).isNull();
    }
}
