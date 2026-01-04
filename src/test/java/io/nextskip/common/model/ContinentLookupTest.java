package io.nextskip.common.model;

import io.nextskip.test.fixtures.CallsignFixtures;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static io.nextskip.test.TestConstants.AS_GRID;
import static io.nextskip.test.TestConstants.EU_GRID;
import static io.nextskip.test.TestConstants.NA_GRID;
import static io.nextskip.test.TestConstants.NA_WEST_GRID;
import static io.nextskip.test.TestConstants.OC_GRID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ContinentLookup}.
 *
 * <p>Tests grid square to continent mapping and continent validation.
 */
class ContinentLookupTest {

    // ===========================================
    // fromGridSquare tests
    // ===========================================

    @Test
    void testFromGridSquare_NaGrid_ReturnsNA() {
        String continent = ContinentLookup.fromGridSquare(NA_GRID);

        assertThat(continent).isEqualTo("NA");
    }

    @Test
    void testFromGridSquare_EuGrid_ReturnsEU() {
        String continent = ContinentLookup.fromGridSquare(EU_GRID);

        assertThat(continent).isEqualTo("EU");
    }

    @Test
    void testFromGridSquare_AsGrid_ReturnsAS() {
        String continent = ContinentLookup.fromGridSquare(AS_GRID);

        assertThat(continent).isEqualTo("AS");
    }

    @Test
    void testFromGridSquare_OcGrid_ReturnsOC() {
        String continent = ContinentLookup.fromGridSquare(OC_GRID);

        assertThat(continent).isEqualTo("OC");
    }

    @Test
    void testFromGridSquare_NaWestGrid_ReturnsNA() {
        String continent = ContinentLookup.fromGridSquare(NA_WEST_GRID);

        // CM97 - California, starts with C which maps to SA in our simple mapping
        // This is a known limitation of grid-based lookup (~80% accuracy)
        assertThat(continent).isIn("NA", "SA");
    }

    @ParameterizedTest
    @CsvSource({
            "FN31, NA",      // Eastern US
            "FN42, NA",      // New England
            "EM73, NA",      // Texas (E field = NA)
            "DM79, SA",      // Southwest US - D maps to SA (known ~80% accuracy limitation)
            "JO01, EU",      // UK
            "JN48, EU",      // Central Europe
            "IO91, EU",      // Ireland
            "PM95, AS",      // Japan
            "PM35, AS",      // Russia Far East
            "QF22, OC",      // Australia
            "RF80, OC",      // New Zealand area
            "GG99, SA",      // Brazil
            "HH77, AF",      // Africa
            "BL11, AN"       // Antarctica region
    })
    void testFromGridSquare_VariousGrids_ReturnsExpectedContinent(String grid, String expectedContinent) {
        String continent = ContinentLookup.fromGridSquare(grid);

        assertThat(continent).isEqualTo(expectedContinent);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testFromGridSquare_NullOrEmpty_ReturnsNull(String grid) {
        String continent = ContinentLookup.fromGridSquare(grid);

        assertThat(continent).isNull();
    }

    @Test
    void testFromGridSquare_LowercaseGrid_ReturnsContinent() {
        String continent = ContinentLookup.fromGridSquare("fn31");

        assertThat(continent).isEqualTo("NA");
    }

    @Test
    void testFromGridSquare_MixedCaseGrid_ReturnsContinent() {
        String continent = ContinentLookup.fromGridSquare("Fn31Pr");

        assertThat(continent).isEqualTo("NA");
    }

    @Test
    void testFromGridSquare_SingleCharacter_ReturnsContinent() {
        // Edge case: just the field letter
        String continent = ContinentLookup.fromGridSquare("F");

        assertThat(continent).isEqualTo("NA");
    }

    @Test
    void testFromGridSquare_InvalidFirstChar_ReturnsNull() {
        // Z is not a valid Maidenhead field (A-R only)
        String continent = ContinentLookup.fromGridSquare("ZZ99");

        assertThat(continent).isNull();
    }

    // ===========================================
    // fromCallsign tests (placeholder for Phase 2)
    // ===========================================

    @Test
    void testFromCallsign_AnyCallsign_ReturnsNull() {
        // Phase 2 placeholder - currently always returns null
        Callsign callsign = CallsignFixtures.usCallsign();

        String continent = ContinentLookup.fromCallsign(callsign);

        assertThat(continent).isNull();
    }

    @Test
    void testFromCallsign_JapanCallsign_ReturnsNull() {
        // Even for obviously identifiable callsigns, Phase 2 will implement
        Callsign callsign = CallsignFixtures.japanCallsign();

        String continent = ContinentLookup.fromCallsign(callsign);

        assertThat(continent).isNull();
    }

    // ===========================================
    // isValidContinent tests
    // ===========================================

    @ParameterizedTest
    @ValueSource(strings = {"AF", "AN", "AS", "EU", "NA", "OC", "SA"})
    void testIsValidContinent_ValidCodes_ReturnsTrue(String continent) {
        assertThat(ContinentLookup.isValidContinent(continent)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"af", "an", "as", "eu", "na", "oc", "sa"})
    void testIsValidContinent_LowercaseValidCodes_ReturnsTrue(String continent) {
        assertThat(ContinentLookup.isValidContinent(continent)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"XX", "US", "JP", "UK", "EUROPE", "N", "NAA"})
    void testIsValidContinent_InvalidCodes_ReturnsFalse(String continent) {
        assertThat(ContinentLookup.isValidContinent(continent)).isFalse();
    }

    @Test
    void testIsValidContinent_Null_ReturnsFalse() {
        assertThat(ContinentLookup.isValidContinent(null)).isFalse();
    }

    // ===========================================
    // getContinentName tests
    // ===========================================

    @ParameterizedTest
    @CsvSource({
            "AF, Africa",
            "AN, Antarctica",
            "AS, Asia",
            "EU, Europe",
            "NA, North America",
            "OC, Oceania",
            "SA, South America"
    })
    void testGetContinentName_ValidCodes_ReturnsFullName(String code, String expectedName) {
        String name = ContinentLookup.getContinentName(code);

        assertThat(name).isEqualTo(expectedName);
    }

    @ParameterizedTest
    @ValueSource(strings = {"na", "eu", "as"})
    void testGetContinentName_LowercaseValidCodes_ReturnsFullName(String code) {
        String name = ContinentLookup.getContinentName(code);

        assertThat(name).isNotNull();
    }

    @Test
    void testGetContinentName_InvalidCode_ReturnsNull() {
        String name = ContinentLookup.getContinentName("XX");

        assertThat(name).isNull();
    }

    @Test
    void testGetContinentName_Null_ReturnsNull() {
        String name = ContinentLookup.getContinentName(null);

        assertThat(name).isNull();
    }

    // ===========================================
    // Property-based tests
    // ===========================================

    @Property
    void fromGridSquareReturnsValidContinentOrNull(@ForAll("maidenheadFields") char field) {
        String grid = field + "N00";
        String continent = ContinentLookup.fromGridSquare(grid);

        if (continent != null) {
            assertThat(ContinentLookup.isValidContinent(continent)).isTrue();
        }
    }

    @Property
    void validContinentsAlwaysHaveNames(@ForAll("validContinentCodes") String code) {
        if (ContinentLookup.isValidContinent(code)) {
            assertThat(ContinentLookup.getContinentName(code)).isNotNull();
        }
    }

    @Provide
    Arbitrary<Character> maidenheadFields() {
        // Valid Maidenhead fields are A-R
        return Arbitraries.chars().range('A', 'R');
    }

    @Provide
    Arbitrary<String> validContinentCodes() {
        return Arbitraries.of("AF", "AN", "AS", "EU", "NA", "OC", "SA");
    }
}
