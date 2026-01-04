package io.nextskip.spots.internal.enrichment;

import io.nextskip.spots.model.Spot;
import io.nextskip.test.fixtures.SpotFixtures;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.CharRange;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.nextskip.test.TestConstants.AS_GRID;
import static io.nextskip.test.TestConstants.DISTANCE_TOLERANCE_KM;
import static io.nextskip.test.TestConstants.EU_GRID;
import static io.nextskip.test.TestConstants.NA_GRID;
import static io.nextskip.test.TestConstants.NA_WEST_GRID;
import static io.nextskip.test.TestConstants.TRANSATLANTIC_DISTANCE_KM;
import static io.nextskip.test.TestConstants.TRANSPACIFIC_DISTANCE_KM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link DistanceEnricher}.
 *
 * <p>Tests Haversine distance calculation between grid squares.
 */
class DistanceEnricherTest {

    private DistanceEnricher enricher;

    @BeforeEach
    void setUp() {
        enricher = new DistanceEnricher();
    }

    @Test
    void testEnrich_ValidGridSquares_CalculatesDistance() {
        Spot spot = SpotFixtures.spot()
                .spotterGrid(NA_GRID)  // FN31 - Boston area
                .spottedGrid(EU_GRID)  // JO01 - UK area
                .distanceKm(null)
                .build();

        Spot enriched = enricher.enrich(spot);

        assertThat(enriched.distanceKm())
                .isNotNull()
                .isCloseTo(TRANSATLANTIC_DISTANCE_KM, within(DISTANCE_TOLERANCE_KM));
    }

    @Test
    void testEnrich_NullSpot_ReturnsNull() {
        Spot result = enricher.enrich(null);

        assertThat(result).isNull();
    }

    @Test
    void testEnrich_AlreadyEnriched_ReturnsUnchanged() {
        Spot spot = SpotFixtures.spot()
                .distanceKm(5500)
                .build();

        Spot enriched = enricher.enrich(spot);

        assertThat(enriched).isSameAs(spot);
    }

    @Test
    void testEnrich_MissingSpotterGrid_ReturnsUnchanged() {
        Spot spot = SpotFixtures.spot()
                .spotterGrid(null)
                .spottedGrid(EU_GRID)
                .distanceKm(null)
                .build();

        Spot enriched = enricher.enrich(spot);

        assertThat(enriched).isSameAs(spot);
        assertThat(enriched.distanceKm()).isNull();
    }

    @Test
    void testEnrich_MissingSpottedGrid_ReturnsUnchanged() {
        Spot spot = SpotFixtures.spot()
                .spotterGrid(NA_GRID)
                .spottedGrid(null)
                .distanceKm(null)
                .build();

        Spot enriched = enricher.enrich(spot);

        assertThat(enriched).isSameAs(spot);
        assertThat(enriched.distanceKm()).isNull();
    }

    @Test
    void testEnrich_BlankSpotterGrid_ReturnsUnchanged() {
        Spot spot = SpotFixtures.spot()
                .spotterGrid("   ")
                .spottedGrid(EU_GRID)
                .distanceKm(null)
                .build();

        Spot enriched = enricher.enrich(spot);

        assertThat(enriched).isSameAs(spot);
    }

    @Test
    void testEnrich_BlankSpottedGrid_ReturnsUnchanged() {
        Spot spot = SpotFixtures.spot()
                .spotterGrid(NA_GRID)
                .spottedGrid("")
                .distanceKm(null)
                .build();

        Spot enriched = enricher.enrich(spot);

        assertThat(enriched).isSameAs(spot);
    }

    @Test
    void testEnrich_InvalidGridFormat_ReturnsUnchanged() {
        Spot spot = SpotFixtures.spot()
                .spotterGrid("INVALID")
                .spottedGrid(EU_GRID)
                .distanceKm(null)
                .build();

        Spot enriched = enricher.enrich(spot);

        assertThat(enriched).isSameAs(spot);
    }

    @Test
    void testEnrich_TransAtlanticPath_CalculatesCorrectly() {
        Spot spot = SpotFixtures.spot()
                .spotterGrid(NA_GRID)  // FN31
                .spottedGrid(EU_GRID)  // JO01
                .distanceKm(null)
                .build();

        Spot enriched = enricher.enrich(spot);

        // Trans-Atlantic should be approximately 5000-6000 km
        assertThat(enriched.distanceKm())
                .isNotNull()
                .isBetween(5000, 6000);
    }

    @Test
    void testEnrich_TransPacificPath_CalculatesCorrectly() {
        Spot spot = SpotFixtures.spot()
                .spotterGrid(NA_WEST_GRID)  // CM97 - West Coast
                .spottedGrid(AS_GRID)       // PM95 - Japan
                .distanceKm(null)
                .build();

        Spot enriched = enricher.enrich(spot);

        // Trans-Pacific should be approximately 8000-9500 km
        assertThat(enriched.distanceKm())
                .isNotNull()
                .isCloseTo(TRANSPACIFIC_DISTANCE_KM, within(500));
    }

    @Test
    void testEnrich_LocalPath_CalculatesCorrectly() {
        Spot spot = SpotFixtures.spot()
                .spotterGrid("FN31")  // Boston
                .spottedGrid("FN42")  // ~100km away
                .distanceKm(null)
                .build();

        Spot enriched = enricher.enrich(spot);

        // Local paths should be under 500 km
        assertThat(enriched.distanceKm())
                .isNotNull()
                .isLessThan(500);
    }

    @Test
    void testEnrich_SameGrid_ReturnsZeroOrNearZero() {
        Spot spot = SpotFixtures.spot()
                .spotterGrid(NA_GRID)
                .spottedGrid(NA_GRID)
                .distanceKm(null)
                .build();

        Spot enriched = enricher.enrich(spot);

        // Same grid should result in zero or near-zero distance
        assertThat(enriched.distanceKm())
                .isNotNull()
                .isLessThanOrEqualTo(50);  // Tolerance for grid center approximation
    }

    @Test
    void testEnrich_PreservesOtherFields() {
        Spot original = SpotFixtures.spot()
                .source("TestSource")
                .band("40m")
                .mode("CW")
                .frequencyHz(7074000L)
                .snr(-15)
                .spotterCall("W1AW")
                .spotterGrid(NA_GRID)
                .spotterContinent("NA")
                .spottedCall("G3ABC")
                .spottedGrid(EU_GRID)
                .spottedContinent("EU")
                .distanceKm(null)
                .build();

        Spot enriched = enricher.enrich(original);

        assertThat(enriched.source()).isEqualTo(original.source());
        assertThat(enriched.band()).isEqualTo(original.band());
        assertThat(enriched.mode()).isEqualTo(original.mode());
        assertThat(enriched.frequencyHz()).isEqualTo(original.frequencyHz());
        assertThat(enriched.snr()).isEqualTo(original.snr());
        assertThat(enriched.spotterCall()).isEqualTo(original.spotterCall());
        assertThat(enriched.spotterContinent()).isEqualTo(original.spotterContinent());
        assertThat(enriched.spottedCall()).isEqualTo(original.spottedCall());
        assertThat(enriched.spottedContinent()).isEqualTo(original.spottedContinent());
    }

    // Property-based tests using jqwik
    // Note: jqwik doesn't use JUnit @BeforeEach, so we create enricher inline

    @Property
    void distanceIsAlwaysNonNegative(
            @ForAll @CharRange(from = 'A', to = 'R') char field1,
            @ForAll @CharRange(from = 'A', to = 'R') char field2,
            @ForAll @IntRange(min = 0, max = 9) int num1,
            @ForAll @IntRange(min = 0, max = 9) int num2) {

        // Generate valid 4-char grids (field + square): e.g., "FN31"
        String grid1 = Character.toString(field1) + "N" + num1 + num2;
        String grid2 = Character.toString(field2) + "N" + num1 + num2;

        DistanceEnricher propertyEnricher = new DistanceEnricher();
        Spot spot = SpotFixtures.spot()
                .spotterGrid(grid1)
                .spottedGrid(grid2)
                .distanceKm(null)
                .build();

        Spot enriched = propertyEnricher.enrich(spot);

        if (enriched.distanceKm() != null) {
            assertThat(enriched.distanceKm()).isGreaterThanOrEqualTo(0);
        }
    }

    @Property
    void distanceIsSymmetric(
            @ForAll @CharRange(from = 'A', to = 'R') char field1,
            @ForAll @CharRange(from = 'A', to = 'R') char field2,
            @ForAll @IntRange(min = 0, max = 9) int num1,
            @ForAll @IntRange(min = 0, max = 9) int num2) {

        // Generate valid 4-char grids: e.g., "FN31"
        String grid1 = Character.toString(field1) + "A" + num1 + num2;
        String grid2 = Character.toString(field2) + "A" + num1 + num2;

        DistanceEnricher propertyEnricher = new DistanceEnricher();
        Spot spot1 = SpotFixtures.spot()
                .spotterGrid(grid1)
                .spottedGrid(grid2)
                .distanceKm(null)
                .build();

        Spot spot2 = SpotFixtures.spot()
                .spotterGrid(grid2)
                .spottedGrid(grid1)
                .distanceKm(null)
                .build();

        Spot enriched1 = propertyEnricher.enrich(spot1);
        Spot enriched2 = propertyEnricher.enrich(spot2);

        if (enriched1.distanceKm() != null && enriched2.distanceKm() != null) {
            assertThat(enriched1.distanceKm()).isEqualTo(enriched2.distanceKm());
        }
    }
}
