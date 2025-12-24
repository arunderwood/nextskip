package io.nextskip.activations.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SotaAssociationMapperTest {

    @ParameterizedTest
    @CsvSource({
            // W1 region (New England)
            "W1, CT",
            "W1/CT, CT",
            "W1/MA, MA",
            "W1/ME, ME",
            "W1/NH, NH",
            "W1/RI, RI",
            "W1/VT, VT",

            // W2 region (New York, New Jersey)
            "W2, NY",
            "W2/NJ, NJ",
            "W2/NY, NY",

            // W3 region (Mid-Atlantic)
            "W3, PA",
            "W3/PA, PA",
            "W3/DE, DE",
            "W3/MD, MD",

            // W4 region (Southeast)
            "W4A, AL",
            "W4C, NC",
            "W4G, GA",
            "W4K, KY",
            "W4T, TN",
            "W4V, VA",

            // W5 region (South Central)
            "W5A, AR",
            "W5L, LA",
            "W5M, MS",
            "W5N, NM",
            "W5O, OK",
            "W5T, TX",

            // W6 region (California)
            "W6, CA",

            // W7 region (Pacific Northwest / Mountain)
            "W7A, AZ",
            "W7I, ID",
            "W7M, MT",
            "W7N, NV",
            "W7O, OR",
            "W7U, UT",
            "W7W, WA",
            "W7Y, WY",

            // W8 region (Great Lakes)
            "W8M, MI",
            "W8O, OH",
            "W8V, WV",

            // W9 region (Central)
            "W9, IL",
            "W9/IL, IL",
            "W9/IN, IN",
            "W9/WI, WI",

            // W0 region (Central Plains / Upper Midwest)
            "W0C, CO",
            "W0K, KS",
            "W0M, MN",
            "W0N, NE",
            "W0D, SD",
            "W0S, SD",
            "W0I, IA"
    })
    void shouldMap_KnownAssociationCodesToStates(String associationCode, String expectedState) {
        Optional<String> result = SotaAssociationMapper.toStateCode(associationCode);

        assertTrue(result.isPresent(), "Expected state for " + associationCode);
        assertEquals(expectedState, result.get());
    }

    @Test
    void shouldMap_W7WToWashington() {
        Optional<String> result = SotaAssociationMapper.toStateCode("W7W");

        assertTrue(result.isPresent());
        assertEquals("WA", result.get());
    }

    @Test
    void shouldMap_W4GToGeorgia() {
        Optional<String> result = SotaAssociationMapper.toStateCode("W4G");

        assertTrue(result.isPresent());
        assertEquals("GA", result.get());
    }

    @Test
    void shouldMap_W0CToColorado() {
        Optional<String> result = SotaAssociationMapper.toStateCode("W0C");

        assertTrue(result.isPresent());
        assertEquals("CO", result.get());
    }

    @Test
    void shouldMap_W6ToCalifornia() {
        Optional<String> result = SotaAssociationMapper.toStateCode("W6");

        assertTrue(result.isPresent());
        assertEquals("CA", result.get());
    }

    @Test
    void shouldHandle_LowercaseInput() {
        Optional<String> result = SotaAssociationMapper.toStateCode("w7w");

        assertTrue(result.isPresent());
        assertEquals("WA", result.get());
    }

    @Test
    void shouldHandle_MixedCaseInput() {
        Optional<String> result = SotaAssociationMapper.toStateCode("w7W");

        assertTrue(result.isPresent());
        assertEquals("WA", result.get());
    }

    @Test
    void shouldHandle_InputWithWhitespace() {
        Optional<String> result = SotaAssociationMapper.toStateCode("  W7W  ");

        assertTrue(result.isPresent());
        assertEquals("WA", result.get());
    }

    @Test
    void shouldReturnEmpty_ForNullInput() {
        Optional<String> result = SotaAssociationMapper.toStateCode(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmpty_ForBlankInput() {
        Optional<String> result = SotaAssociationMapper.toStateCode("   ");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmpty_ForEmptyString() {
        Optional<String> result = SotaAssociationMapper.toStateCode("");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmpty_ForUnknownCode() {
        Optional<String> result = SotaAssociationMapper.toStateCode("W9Z");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmpty_ForInternationalAssociations() {
        // Test common international SOTA associations
        assertTrue(SotaAssociationMapper.toStateCode("VK2").isEmpty()); // Australia
        assertTrue(SotaAssociationMapper.toStateCode("G/LD").isEmpty()); // England
        assertTrue(SotaAssociationMapper.toStateCode("JA").isEmpty()); // Japan
        assertTrue(SotaAssociationMapper.toStateCode("DL/NS").isEmpty()); // Germany
        assertTrue(SotaAssociationMapper.toStateCode("ZL").isEmpty()); // New Zealand
    }

    @Test
    void shouldHandle_FullSummitReferenceFormat() {
        // Even though full references like "W7W/LC-001" won't match,
        // this test documents expected behavior
        Optional<String> result = SotaAssociationMapper.toStateCode("W7W/LC-001");

        assertTrue(result.isEmpty(), "Full summit references should not match");
    }

    @Test
    void shouldHandle_AllW1SubRegions() {
        assertEquals("CT", SotaAssociationMapper.toStateCode("W1/CT").orElse(null));
        assertEquals("MA", SotaAssociationMapper.toStateCode("W1/MA").orElse(null));
        assertEquals("ME", SotaAssociationMapper.toStateCode("W1/ME").orElse(null));
        assertEquals("NH", SotaAssociationMapper.toStateCode("W1/NH").orElse(null));
        assertEquals("RI", SotaAssociationMapper.toStateCode("W1/RI").orElse(null));
        assertEquals("VT", SotaAssociationMapper.toStateCode("W1/VT").orElse(null));
    }

    @Test
    void shouldHandle_AllW7States() {
        assertEquals("AZ", SotaAssociationMapper.toStateCode("W7A").orElse(null));
        assertEquals("ID", SotaAssociationMapper.toStateCode("W7I").orElse(null));
        assertEquals("MT", SotaAssociationMapper.toStateCode("W7M").orElse(null));
        assertEquals("NV", SotaAssociationMapper.toStateCode("W7N").orElse(null));
        assertEquals("OR", SotaAssociationMapper.toStateCode("W7O").orElse(null));
        assertEquals("UT", SotaAssociationMapper.toStateCode("W7U").orElse(null));
        assertEquals("WA", SotaAssociationMapper.toStateCode("W7W").orElse(null));
        assertEquals("WY", SotaAssociationMapper.toStateCode("W7Y").orElse(null));
    }

    @Test
    void shouldHandle_AllW5States() {
        assertEquals("AR", SotaAssociationMapper.toStateCode("W5A").orElse(null));
        assertEquals("LA", SotaAssociationMapper.toStateCode("W5L").orElse(null));
        assertEquals("MS", SotaAssociationMapper.toStateCode("W5M").orElse(null));
        assertEquals("NM", SotaAssociationMapper.toStateCode("W5N").orElse(null));
        assertEquals("OK", SotaAssociationMapper.toStateCode("W5O").orElse(null));
        assertEquals("TX", SotaAssociationMapper.toStateCode("W5T").orElse(null));
    }
}
