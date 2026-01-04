package io.nextskip.spots.internal.enrichment;

import io.nextskip.spots.model.Spot;
import io.nextskip.test.fixtures.SpotFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.nextskip.test.TestConstants.AS_GRID;
import static io.nextskip.test.TestConstants.EU_GRID;
import static io.nextskip.test.TestConstants.NA_GRID;
import static io.nextskip.test.TestConstants.OC_GRID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ContinentEnricher}.
 *
 * <p>Tests continent enrichment based on grid square field.
 */
class ContinentEnricherTest {

    private ContinentEnricher enricher;

    @BeforeEach
    void setUp() {
        enricher = new ContinentEnricher();
    }

    @Test
    void testEnrich_ValidGrids_AddsContinents() {
        Spot spot = SpotFixtures.spot()
                .spotterGrid(NA_GRID)  // FN31 -> NA
                .spottedGrid(EU_GRID)  // JO01 -> EU
                .spotterContinent(null)
                .spottedContinent(null)
                .build();

        Spot enriched = enricher.enrich(spot);

        assertThat(enriched.spotterContinent()).isEqualTo("NA");
        assertThat(enriched.spottedContinent()).isEqualTo("EU");
    }

    @Test
    void testEnrich_NullSpot_ReturnsNull() {
        Spot result = enricher.enrich(null);

        assertThat(result).isNull();
    }

    @Test
    void testEnrich_AlreadyEnriched_ReturnsUnchanged() {
        Spot spot = SpotFixtures.spot()
                .spotterContinent("NA")
                .spottedContinent("EU")
                .build();

        Spot enriched = enricher.enrich(spot);

        assertThat(enriched).isSameAs(spot);
    }

    @Test
    void testEnrich_MissingSpotterGrid_EnrichesSpottedOnly() {
        Spot spot = SpotFixtures.spot()
                .spotterGrid(null)
                .spottedGrid(EU_GRID)
                .spotterContinent(null)
                .spottedContinent(null)
                .build();

        Spot enriched = enricher.enrich(spot);

        assertThat(enriched.spotterContinent()).isNull();
        assertThat(enriched.spottedContinent()).isEqualTo("EU");
    }

    @Test
    void testEnrich_MissingSpottedGrid_EnrichesSpotterOnly() {
        Spot spot = SpotFixtures.spot()
                .spotterGrid(NA_GRID)
                .spottedGrid(null)
                .spotterContinent(null)
                .spottedContinent(null)
                .build();

        Spot enriched = enricher.enrich(spot);

        assertThat(enriched.spotterContinent()).isEqualTo("NA");
        assertThat(enriched.spottedContinent()).isNull();
    }

    @Test
    void testEnrich_NeitherGridAvailable_ReturnsUnchanged() {
        Spot spot = SpotFixtures.spot()
                .spotterGrid(null)
                .spottedGrid(null)
                .spotterContinent(null)
                .spottedContinent(null)
                .build();

        Spot enriched = enricher.enrich(spot);

        assertThat(enriched).isSameAs(spot);
    }

    @Test
    void testEnrich_PartialEnrichment_SpotterAlreadySet_CompletesRemaining() {
        Spot spot = SpotFixtures.spot()
                .spotterGrid(NA_GRID)
                .spottedGrid(EU_GRID)
                .spotterContinent("NA")  // Already set
                .spottedContinent(null)  // Needs enrichment
                .build();

        Spot enriched = enricher.enrich(spot);

        assertThat(enriched.spotterContinent()).isEqualTo("NA");
        assertThat(enriched.spottedContinent()).isEqualTo("EU");
    }

    @Test
    void testEnrich_PartialEnrichment_SpottedAlreadySet_CompletesRemaining() {
        Spot spot = SpotFixtures.spot()
                .spotterGrid(NA_GRID)
                .spottedGrid(EU_GRID)
                .spotterContinent(null)  // Needs enrichment
                .spottedContinent("EU")  // Already set
                .build();

        Spot enriched = enricher.enrich(spot);

        assertThat(enriched.spotterContinent()).isEqualTo("NA");
        assertThat(enriched.spottedContinent()).isEqualTo("EU");
    }

    @Test
    void testEnrich_AsiaGrid_ReturnsAS() {
        Spot spot = SpotFixtures.spot()
                .spotterGrid(AS_GRID)  // PM95 -> AS
                .spottedGrid(NA_GRID)
                .spotterContinent(null)
                .spottedContinent(null)
                .build();

        Spot enriched = enricher.enrich(spot);

        assertThat(enriched.spotterContinent()).isEqualTo("AS");
    }

    @Test
    void testEnrich_OceaniaGrid_ReturnsOC() {
        Spot spot = SpotFixtures.spot()
                .spotterGrid(OC_GRID)  // QF22 -> OC
                .spottedGrid(NA_GRID)
                .spotterContinent(null)
                .spottedContinent(null)
                .build();

        Spot enriched = enricher.enrich(spot);

        assertThat(enriched.spotterContinent()).isEqualTo("OC");
    }

    @Test
    void testEnrich_SouthAmericaGrid_ReturnsSA() {
        Spot spot = SpotFixtures.spot()
                .spotterGrid("GG99")  // South America
                .spottedGrid(NA_GRID)
                .spotterContinent(null)
                .spottedContinent(null)
                .build();

        Spot enriched = enricher.enrich(spot);

        assertThat(enriched.spotterContinent()).isEqualTo("SA");
    }

    @Test
    void testEnrich_AfricaGrid_ReturnsAF() {
        // H field -> Africa in our simple grid-based mapping
        Spot spot = SpotFixtures.spot()
                .spotterGrid("HH77")
                .spottedGrid(NA_GRID)
                .spotterContinent(null)
                .spottedContinent(null)
                .build();

        Spot enriched = enricher.enrich(spot);

        assertThat(enriched.spotterContinent()).isEqualTo("AF");
    }

    @Test
    void testEnrich_EmptyGridString_TreatedAsNull() {
        Spot spot = SpotFixtures.spot()
                .spotterGrid("")
                .spottedGrid(EU_GRID)
                .spotterContinent(null)
                .spottedContinent(null)
                .build();

        Spot enriched = enricher.enrich(spot);

        assertThat(enriched.spotterContinent()).isNull();
        assertThat(enriched.spottedContinent()).isEqualTo("EU");
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
                .spotterContinent(null)
                .spottedCall("G3ABC")
                .spottedGrid(EU_GRID)
                .spottedContinent(null)
                .distanceKm(5500)
                .build();

        Spot enriched = enricher.enrich(original);

        assertThat(enriched.source()).isEqualTo(original.source());
        assertThat(enriched.band()).isEqualTo(original.band());
        assertThat(enriched.mode()).isEqualTo(original.mode());
        assertThat(enriched.frequencyHz()).isEqualTo(original.frequencyHz());
        assertThat(enriched.snr()).isEqualTo(original.snr());
        assertThat(enriched.spotterCall()).isEqualTo(original.spotterCall());
        assertThat(enriched.spottedCall()).isEqualTo(original.spottedCall());
        assertThat(enriched.distanceKm()).isEqualTo(original.distanceKm());
    }
}
