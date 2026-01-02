package io.nextskip.activations.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParkTest {

    private static final String REFERENCE_K_0817 = "K-0817";
    private static final String NAME_ROCKY_MOUNTAIN = "Rocky Mountain National Park";
    private static final String REGION_CODE_CO = "CO";
    private static final String COUNTRY_CODE_US = "US";
    private static final String GRID_DM79 = "DM79";
    private static final double LATITUDE_40_3428 = 40.3428;
    private static final double LONGITUDE_NEG_105_6836 = -105.6836;

    @Test
    void shouldCreate_ParkWithAllFields() {
        Park park = new Park(
                REFERENCE_K_0817,
                NAME_ROCKY_MOUNTAIN,
                REGION_CODE_CO,
                COUNTRY_CODE_US,
                GRID_DM79,
                LATITUDE_40_3428,
                LONGITUDE_NEG_105_6836
        );

        assertEquals(REFERENCE_K_0817, park.reference());
        assertEquals(NAME_ROCKY_MOUNTAIN, park.name());
        assertEquals(REGION_CODE_CO, park.regionCode());
        assertEquals(COUNTRY_CODE_US, park.countryCode());
        assertEquals(GRID_DM79, park.grid());
        assertEquals(LATITUDE_40_3428, park.latitude());
        assertEquals(LONGITUDE_NEG_105_6836, park.longitude());
    }

    @Test
    void shouldCreate_ParkWithNullOptionalFields() {
        Park park = new Park(
                REFERENCE_K_0817,
                NAME_ROCKY_MOUNTAIN,
                null,  // regionCode
                null,  // countryCode
                null,  // grid
                null,  // latitude
                null   // longitude
        );

        assertEquals(REFERENCE_K_0817, park.reference());
        assertEquals(NAME_ROCKY_MOUNTAIN, park.name());
        assertNull(park.regionCode());
        assertNull(park.countryCode());
        assertNull(park.grid());
        assertNull(park.latitude());
        assertNull(park.longitude());
    }

    @Test
    void shouldImplement_ActivationLocation() {
        Park park = new Park(
                REFERENCE_K_0817, NAME_ROCKY_MOUNTAIN, REGION_CODE_CO,
                COUNTRY_CODE_US, GRID_DM79, LATITUDE_40_3428, LONGITUDE_NEG_105_6836);

        assertTrue(park instanceof ActivationLocation);
        assertEquals(REFERENCE_K_0817, park.reference());
        assertEquals(NAME_ROCKY_MOUNTAIN, park.name());
        assertEquals(REGION_CODE_CO, park.regionCode());
    }

    @Test
    void shouldThrow_ExceptionWhenReferenceIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Park(null, NAME_ROCKY_MOUNTAIN, REGION_CODE_CO,
                        COUNTRY_CODE_US, GRID_DM79, LATITUDE_40_3428, LONGITUDE_NEG_105_6836)
        );
        assertEquals("Park reference cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldThrow_ExceptionWhenReferenceIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Park("  ", NAME_ROCKY_MOUNTAIN, REGION_CODE_CO,
                        COUNTRY_CODE_US, GRID_DM79, LATITUDE_40_3428, LONGITUDE_NEG_105_6836)
        );
        assertEquals("Park reference cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldThrow_ExceptionWhenNameIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Park(REFERENCE_K_0817, null, REGION_CODE_CO,
                        COUNTRY_CODE_US, GRID_DM79, LATITUDE_40_3428, LONGITUDE_NEG_105_6836)
        );
        assertEquals("Park name cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldThrow_ExceptionWhenNameIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Park(REFERENCE_K_0817, "", REGION_CODE_CO,
                        COUNTRY_CODE_US, GRID_DM79, LATITUDE_40_3428, LONGITUDE_NEG_105_6836)
        );
        assertEquals("Park name cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldBeEqual_WhenAllFieldsMatch() {
        Park park1 = new Park(
                REFERENCE_K_0817, NAME_ROCKY_MOUNTAIN, REGION_CODE_CO,
                COUNTRY_CODE_US, GRID_DM79, LATITUDE_40_3428, LONGITUDE_NEG_105_6836);
        Park park2 = new Park(
                REFERENCE_K_0817, NAME_ROCKY_MOUNTAIN, REGION_CODE_CO,
                COUNTRY_CODE_US, GRID_DM79, LATITUDE_40_3428, LONGITUDE_NEG_105_6836);

        assertEquals(park1, park2);
        assertEquals(park1.hashCode(), park2.hashCode());
    }

    @Test
    void shouldNotBeEqual_WhenReferencesDiffer() {
        Park park1 = new Park(
                REFERENCE_K_0817, NAME_ROCKY_MOUNTAIN, REGION_CODE_CO,
                COUNTRY_CODE_US, GRID_DM79, LATITUDE_40_3428, LONGITUDE_NEG_105_6836);
        Park park2 = new Park(
                "K-1234", NAME_ROCKY_MOUNTAIN, REGION_CODE_CO,
                COUNTRY_CODE_US, GRID_DM79, LATITUDE_40_3428, LONGITUDE_NEG_105_6836);

        assertNotEquals(park1, park2);
    }

    @Test
    void shouldHandle_USStateReferences() {
        Park park = new Park(
                REFERENCE_K_0817, NAME_ROCKY_MOUNTAIN, REGION_CODE_CO,
                COUNTRY_CODE_US, GRID_DM79, LATITUDE_40_3428, LONGITUDE_NEG_105_6836);

        assertEquals(REGION_CODE_CO, park.regionCode());
        assertEquals(COUNTRY_CODE_US, park.countryCode());
    }

    @Test
    void shouldHandle_CanadianProvincialReferences() {
        Park park = new Park("VE-0001", "Algonquin Provincial Park", "ON", "CA", "FN04", 45.5, -78.3);

        assertEquals("ON", park.regionCode());
        assertEquals("CA", park.countryCode());
    }
}
