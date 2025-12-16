package io.nextskip.activations.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParkTest {

    @Test
    void shouldCreateParkWithAllFields() {
        Park park = new Park(
                "K-0817",
                "Rocky Mountain National Park",
                "CO",
                "US",
                "DM79",
                40.3428,
                -105.6836
        );

        assertEquals("K-0817", park.reference());
        assertEquals("Rocky Mountain National Park", park.name());
        assertEquals("CO", park.regionCode());
        assertEquals("US", park.countryCode());
        assertEquals("DM79", park.grid());
        assertEquals(40.3428, park.latitude());
        assertEquals(-105.6836, park.longitude());
    }

    @Test
    void shouldCreateParkWithNullOptionalFields() {
        Park park = new Park(
                "K-0817",
                "Rocky Mountain National Park",
                null,  // regionCode
                null,  // countryCode
                null,  // grid
                null,  // latitude
                null   // longitude
        );

        assertEquals("K-0817", park.reference());
        assertEquals("Rocky Mountain National Park", park.name());
        assertNull(park.regionCode());
        assertNull(park.countryCode());
        assertNull(park.grid());
        assertNull(park.latitude());
        assertNull(park.longitude());
    }

    @Test
    void shouldImplementActivationLocation() {
        Park park = new Park("K-0817", "Rocky Mountain National Park", "CO", "US", "DM79", 40.3428, -105.6836);

        assertTrue(park instanceof ActivationLocation);
        assertEquals("K-0817", park.reference());
        assertEquals("Rocky Mountain National Park", park.name());
        assertEquals("CO", park.regionCode());
    }

    @Test
    void shouldThrowExceptionWhenReferenceIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Park(null, "Rocky Mountain National Park", "CO", "US", "DM79", 40.3428, -105.6836)
        );
        assertEquals("Park reference cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenReferenceIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Park("  ", "Rocky Mountain National Park", "CO", "US", "DM79", 40.3428, -105.6836)
        );
        assertEquals("Park reference cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenNameIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Park("K-0817", null, "CO", "US", "DM79", 40.3428, -105.6836)
        );
        assertEquals("Park name cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenNameIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Park("K-0817", "", "CO", "US", "DM79", 40.3428, -105.6836)
        );
        assertEquals("Park name cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldBeEqualWhenAllFieldsMatch() {
        Park park1 = new Park("K-0817", "Rocky Mountain National Park", "CO", "US", "DM79", 40.3428, -105.6836);
        Park park2 = new Park("K-0817", "Rocky Mountain National Park", "CO", "US", "DM79", 40.3428, -105.6836);

        assertEquals(park1, park2);
        assertEquals(park1.hashCode(), park2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenReferencesDiffer() {
        Park park1 = new Park("K-0817", "Rocky Mountain National Park", "CO", "US", "DM79", 40.3428, -105.6836);
        Park park2 = new Park("K-1234", "Rocky Mountain National Park", "CO", "US", "DM79", 40.3428, -105.6836);

        assertNotEquals(park1, park2);
    }

    @Test
    void shouldHandleUSStateReferences() {
        Park park = new Park("K-0817", "Rocky Mountain National Park", "CO", "US", "DM79", 40.3428, -105.6836);

        assertEquals("CO", park.regionCode());
        assertEquals("US", park.countryCode());
    }

    @Test
    void shouldHandleCanadianProvincialReferences() {
        Park park = new Park("VE-0001", "Algonquin Provincial Park", "ON", "CA", "FN04", 45.5, -78.3);

        assertEquals("ON", park.regionCode());
        assertEquals("CA", park.countryCode());
    }
}
