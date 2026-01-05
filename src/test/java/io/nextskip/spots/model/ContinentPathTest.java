package io.nextskip.spots.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ContinentPath} enum.
 *
 * <p>Tests cover path matching (bidirectional), factory method, and display formatting.
 */
class ContinentPathTest {

    // =========================================================================
    // Enum Values Tests
    // =========================================================================

    @Nested
    class EnumValuesTests {

        @Test
        void testEnumValues_HaveSixMajorPaths() {
            assertThat(ContinentPath.values())
                    .as("Should have exactly 6 major HF paths")
                    .hasSize(6);
        }

        @Test
        void testNA_EU_HasCorrectProperties() {
            ContinentPath path = ContinentPath.NA_EU;

            assertThat(path.getContinent1()).isEqualTo("NA");
            assertThat(path.getContinent2()).isEqualTo("EU");
            assertThat(path.getDisplayName()).isEqualTo("Trans-Atlantic");
        }

        @Test
        void testNA_AS_HasCorrectProperties() {
            ContinentPath path = ContinentPath.NA_AS;

            assertThat(path.getContinent1()).isEqualTo("NA");
            assertThat(path.getContinent2()).isEqualTo("AS");
            assertThat(path.getDisplayName()).isEqualTo("Trans-Pacific");
        }

        @Test
        void testEU_AS_HasCorrectProperties() {
            ContinentPath path = ContinentPath.EU_AS;

            assertThat(path.getContinent1()).isEqualTo("EU");
            assertThat(path.getContinent2()).isEqualTo("AS");
            assertThat(path.getDisplayName()).isEqualTo("Europe-Asia");
        }

        @Test
        void testNA_OC_HasCorrectProperties() {
            ContinentPath path = ContinentPath.NA_OC;

            assertThat(path.getContinent1()).isEqualTo("NA");
            assertThat(path.getContinent2()).isEqualTo("OC");
            assertThat(path.getDisplayName()).isEqualTo("North America-Oceania");
        }

        @Test
        void testEU_AF_HasCorrectProperties() {
            ContinentPath path = ContinentPath.EU_AF;

            assertThat(path.getContinent1()).isEqualTo("EU");
            assertThat(path.getContinent2()).isEqualTo("AF");
            assertThat(path.getDisplayName()).isEqualTo("Europe-Africa");
        }

        @Test
        void testNA_SA_HasCorrectProperties() {
            ContinentPath path = ContinentPath.NA_SA;

            assertThat(path.getContinent1()).isEqualTo("NA");
            assertThat(path.getContinent2()).isEqualTo("SA");
            assertThat(path.getDisplayName()).isEqualTo("North-South America");
        }
    }

    // =========================================================================
    // matches() Tests
    // =========================================================================

    @Nested
    class MatchesTests {

        @ParameterizedTest
        @CsvSource({
                "NA, EU, true",
                "EU, NA, true",
                "na, eu, true",
                "Na, Eu, true",
                "NA, AS, false",
                "EU, EU, false"
        })
        void testNA_EU_Matches_ForwardAndReverse(String c1, String c2, boolean expected) {
            assertThat(ContinentPath.NA_EU.matches(c1, c2))
                    .as("NA_EU.matches(%s, %s) should be %s", c1, c2, expected)
                    .isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({
                "NA, AS, true",
                "AS, NA, true",
                "EU, AS, false"
        })
        void testNA_AS_Matches_ForwardAndReverse(String c1, String c2, boolean expected) {
            assertThat(ContinentPath.NA_AS.matches(c1, c2))
                    .as("NA_AS.matches(%s, %s) should be %s", c1, c2, expected)
                    .isEqualTo(expected);
        }

        @Test
        void testMatches_WithNullSpotterContinent_ReturnsFalse() {
            assertThat(ContinentPath.NA_EU.matches(null, "EU"))
                    .as("Should return false when spotter continent is null")
                    .isFalse();
        }

        @Test
        void testMatches_WithNullSpottedContinent_ReturnsFalse() {
            assertThat(ContinentPath.NA_EU.matches("NA", null))
                    .as("Should return false when spotted continent is null")
                    .isFalse();
        }

        @Test
        void testMatches_WithBothNull_ReturnsFalse() {
            assertThat(ContinentPath.NA_EU.matches(null, null))
                    .as("Should return false when both continents are null")
                    .isFalse();
        }

        @Test
        void testMatches_IsCaseInsensitive() {
            assertThat(ContinentPath.NA_EU.matches("na", "eu")).isTrue();
            assertThat(ContinentPath.NA_EU.matches("NA", "EU")).isTrue();
            assertThat(ContinentPath.NA_EU.matches("Na", "Eu")).isTrue();
        }

        @Test
        void testMatches_SameContinentBothSides_ReturnsFalse() {
            // Same continent on both sides should not match any path
            for (ContinentPath path : ContinentPath.values()) {
                assertThat(path.matches("NA", "NA"))
                        .as("%s should not match same continent on both sides", path)
                        .isFalse();
                assertThat(path.matches("EU", "EU")).isFalse();
            }
        }
    }

    // =========================================================================
    // fromContinents() Tests
    // =========================================================================

    @Nested
    class FromContinentsTests {

        @Test
        void testFromContinents_NA_EU_FindsTransAtlantic() {
            Optional<ContinentPath> result = ContinentPath.fromContinents("NA", "EU");

            assertThat(result)
                    .as("Should find Trans-Atlantic path for NA-EU")
                    .contains(ContinentPath.NA_EU);
        }

        @Test
        void testFromContinents_EU_NA_FindsTransAtlantic() {
            Optional<ContinentPath> result = ContinentPath.fromContinents("EU", "NA");

            assertThat(result)
                    .as("Should find Trans-Atlantic path for EU-NA (reversed)")
                    .contains(ContinentPath.NA_EU);
        }

        @ParameterizedTest
        @CsvSource({
                "NA, AS, NA_AS",
                "AS, NA, NA_AS",
                "EU, AS, EU_AS",
                "NA, OC, NA_OC",
                "EU, AF, EU_AF",
                "NA, SA, NA_SA"
        })
        void testFromContinents_FindsCorrectPath(String c1, String c2, String expectedPath) {
            Optional<ContinentPath> result = ContinentPath.fromContinents(c1, c2);

            assertThat(result)
                    .as("fromContinents(%s, %s) should find %s", c1, c2, expectedPath)
                    .contains(ContinentPath.valueOf(expectedPath));
        }

        @Test
        void testFromContinents_UnknownPath_ReturnsEmpty() {
            // Africa to Oceania is not a defined major path
            Optional<ContinentPath> result = ContinentPath.fromContinents("AF", "OC");

            assertThat(result)
                    .as("Should return empty for undefined path AF-OC")
                    .isEmpty();
        }

        @Test
        void testFromContinents_SameContinents_ReturnsEmpty() {
            Optional<ContinentPath> result = ContinentPath.fromContinents("NA", "NA");

            assertThat(result)
                    .as("Should return empty for same continent")
                    .isEmpty();
        }

        @ParameterizedTest
        @NullSource
        void testFromContinents_NullFirstContinent_ReturnsEmpty(String c1) {
            Optional<ContinentPath> result = ContinentPath.fromContinents(c1, "EU");

            assertThat(result)
                    .as("Should return empty when first continent is null")
                    .isEmpty();
        }

        @ParameterizedTest
        @NullSource
        void testFromContinents_NullSecondContinent_ReturnsEmpty(String c2) {
            Optional<ContinentPath> result = ContinentPath.fromContinents("NA", c2);

            assertThat(result)
                    .as("Should return empty when second continent is null")
                    .isEmpty();
        }

        @Test
        void testFromContinents_IsCaseInsensitive() {
            assertThat(ContinentPath.fromContinents("na", "eu")).contains(ContinentPath.NA_EU);
            assertThat(ContinentPath.fromContinents("Na", "Eu")).contains(ContinentPath.NA_EU);
        }
    }

    // =========================================================================
    // toPathString() Tests
    // =========================================================================

    @Nested
    class ToPathStringTests {

        @Test
        void testToPathString_FormatsCorrectly() {
            assertThat(ContinentPath.NA_EU.toPathString()).isEqualTo("NA↔EU");
            assertThat(ContinentPath.NA_AS.toPathString()).isEqualTo("NA↔AS");
            assertThat(ContinentPath.EU_AS.toPathString()).isEqualTo("EU↔AS");
        }

        @Test
        void testToPathString_AllPathsHaveBidirectionalArrow() {
            for (ContinentPath path : ContinentPath.values()) {
                assertThat(path.toPathString())
                        .as("%s toPathString should contain bidirectional arrow", path)
                        .contains("↔");
            }
        }
    }

    // =========================================================================
    // toString() Tests
    // =========================================================================

    @Nested
    class ToStringTests {

        @Test
        void testToString_IncludesDisplayNameAndPath() {
            String result = ContinentPath.NA_EU.toString();

            assertThat(result)
                    .contains("Trans-Atlantic")
                    .contains("NA↔EU");
        }

        @Test
        void testToString_FormatsWithParentheses() {
            String result = ContinentPath.NA_AS.toString();

            assertThat(result).isEqualTo("Trans-Pacific (NA↔AS)");
        }
    }

    // =========================================================================
    // Coverage Tests for All Paths
    // =========================================================================

    @Nested
    class AllPathsCoverageTests {

        @Test
        void testAllPaths_HaveNonNullContinent1() {
            for (ContinentPath path : ContinentPath.values()) {
                assertThat(path.getContinent1())
                        .as("%s continent1 should not be null", path)
                        .isNotNull()
                        .hasSize(2);
            }
        }

        @Test
        void testAllPaths_HaveNonNullContinent2() {
            for (ContinentPath path : ContinentPath.values()) {
                assertThat(path.getContinent2())
                        .as("%s continent2 should not be null", path)
                        .isNotNull()
                        .hasSize(2);
            }
        }

        @Test
        void testAllPaths_HaveNonEmptyDisplayName() {
            for (ContinentPath path : ContinentPath.values()) {
                assertThat(path.getDisplayName())
                        .as("%s displayName should not be empty", path)
                        .isNotBlank();
            }
        }

        @Test
        void testAllPaths_HaveDifferentContinentsOnEachEnd() {
            for (ContinentPath path : ContinentPath.values()) {
                assertThat(path.getContinent1())
                        .as("%s should connect different continents", path)
                        .isNotEqualTo(path.getContinent2());
            }
        }
    }
}
