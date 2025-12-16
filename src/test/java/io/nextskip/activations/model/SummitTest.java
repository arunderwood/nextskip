package io.nextskip.activations.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SummitTest {

    @Test
    void shouldCreateSummitWithAllFields() {
        Summit summit = new Summit(
                "W7W/LC-001",
                "Mount Saint Helens",
                "WA",
                "W7W"
        );

        assertEquals("W7W/LC-001", summit.reference());
        assertEquals("Mount Saint Helens", summit.name());
        assertEquals("WA", summit.regionCode());
        assertEquals("W7W", summit.associationCode());
    }

    @Test
    void shouldCreateSummitWithNullOptionalFields() {
        Summit summit = new Summit(
                "W7W/LC-001",
                "Mount Saint Helens",
                null,  // regionCode
                null   // associationCode
        );

        assertEquals("W7W/LC-001", summit.reference());
        assertEquals("Mount Saint Helens", summit.name());
        assertNull(summit.regionCode());
        assertNull(summit.associationCode());
    }

    @Test
    void shouldImplementActivationLocation() {
        Summit summit = new Summit("W7W/LC-001", "Mount Saint Helens", "WA", "W7W");

        assertTrue(summit instanceof ActivationLocation);
        assertEquals("W7W/LC-001", summit.reference());
        assertEquals("Mount Saint Helens", summit.name());
        assertEquals("WA", summit.regionCode());
    }

    @Test
    void shouldThrowExceptionWhenReferenceIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Summit(null, "Mount Saint Helens", "WA", "W7W")
        );
        assertEquals("Summit reference cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenReferenceIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Summit("  ", "Mount Saint Helens", "WA", "W7W")
        );
        assertEquals("Summit reference cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenNameIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Summit("W7W/LC-001", null, "WA", "W7W")
        );
        assertEquals("Summit name cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenNameIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Summit("W7W/LC-001", "", "WA", "W7W")
        );
        assertEquals("Summit name cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldBeEqualWhenAllFieldsMatch() {
        Summit summit1 = new Summit("W7W/LC-001", "Mount Saint Helens", "WA", "W7W");
        Summit summit2 = new Summit("W7W/LC-001", "Mount Saint Helens", "WA", "W7W");

        assertEquals(summit1, summit2);
        assertEquals(summit1.hashCode(), summit2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenReferencesDiffer() {
        Summit summit1 = new Summit("W7W/LC-001", "Mount Saint Helens", "WA", "W7W");
        Summit summit2 = new Summit("W7W/LC-002", "Mount Adams", "WA", "W7W");

        assertNotEquals(summit1, summit2);
    }

    @Test
    void shouldHandleW7RegionSummits() {
        Summit summit = new Summit("W7W/LC-001", "Mount Saint Helens", "WA", "W7W");

        assertEquals("WA", summit.regionCode());
        assertEquals("W7W", summit.associationCode());
    }

    @Test
    void shouldHandleW4RegionSummits() {
        Summit summit = new Summit("W4G/NG-001", "Brasstown Bald", "GA", "W4G");

        assertEquals("GA", summit.regionCode());
        assertEquals("W4G", summit.associationCode());
    }

    @Test
    void shouldHandleW6RegionSummits() {
        Summit summit = new Summit("W6/CT-001", "Mount Whitney", "CA", "W6");

        assertEquals("CA", summit.regionCode());
        assertEquals("W6", summit.associationCode());
    }

    @Test
    void shouldHandleInternationalSummits() {
        Summit summit = new Summit("G/LD-001", "Scafell Pike", null, "G/LD");

        assertNull(summit.regionCode());
        assertEquals("G/LD", summit.associationCode());
    }
}
