package io.nextskip.activations.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SummitTest {

    private static final String REFERENCE_W7W_LC_001 = "W7W/LC-001";
    private static final String NAME_MOUNT_SAINT_HELENS = "Mount Saint Helens";
    private static final String REGION_CODE_WA = "WA";
    private static final String ASSOCIATION_CODE_W7W = "W7W";

    @Test
    void shouldCreate_SummitWithAllFields() {
        Summit summit = new Summit(
                REFERENCE_W7W_LC_001,
                NAME_MOUNT_SAINT_HELENS,
                REGION_CODE_WA,
                ASSOCIATION_CODE_W7W
        );

        assertEquals(REFERENCE_W7W_LC_001, summit.reference());
        assertEquals(NAME_MOUNT_SAINT_HELENS, summit.name());
        assertEquals(REGION_CODE_WA, summit.regionCode());
        assertEquals(ASSOCIATION_CODE_W7W, summit.associationCode());
    }

    @Test
    void shouldCreate_SummitWithNullOptionalFields() {
        Summit summit = new Summit(
                REFERENCE_W7W_LC_001,
                NAME_MOUNT_SAINT_HELENS,
                null,  // regionCode
                null   // associationCode
        );

        assertEquals(REFERENCE_W7W_LC_001, summit.reference());
        assertEquals(NAME_MOUNT_SAINT_HELENS, summit.name());
        assertNull(summit.regionCode());
        assertNull(summit.associationCode());
    }

    @Test
    void shouldImplement_ActivationLocation() {
        Summit summit = new Summit(REFERENCE_W7W_LC_001, NAME_MOUNT_SAINT_HELENS, REGION_CODE_WA, ASSOCIATION_CODE_W7W);

        assertTrue(summit instanceof ActivationLocation);
        assertEquals(REFERENCE_W7W_LC_001, summit.reference());
        assertEquals(NAME_MOUNT_SAINT_HELENS, summit.name());
        assertEquals(REGION_CODE_WA, summit.regionCode());
    }

    @Test
    void shouldThrow_ExceptionWhenReferenceIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Summit(null, NAME_MOUNT_SAINT_HELENS, REGION_CODE_WA, ASSOCIATION_CODE_W7W)
        );
        assertEquals("Summit reference cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldThrow_ExceptionWhenReferenceIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Summit("  ", NAME_MOUNT_SAINT_HELENS, REGION_CODE_WA, ASSOCIATION_CODE_W7W)
        );
        assertEquals("Summit reference cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldThrow_ExceptionWhenNameIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Summit(REFERENCE_W7W_LC_001, null, REGION_CODE_WA, ASSOCIATION_CODE_W7W)
        );
        assertEquals("Summit name cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldThrow_ExceptionWhenNameIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Summit(REFERENCE_W7W_LC_001, "", REGION_CODE_WA, ASSOCIATION_CODE_W7W)
        );
        assertEquals("Summit name cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldBeEqual_WhenAllFieldsMatch() {
        Summit summit1 = new Summit(
                REFERENCE_W7W_LC_001, NAME_MOUNT_SAINT_HELENS, REGION_CODE_WA, ASSOCIATION_CODE_W7W);
        Summit summit2 = new Summit(
                REFERENCE_W7W_LC_001, NAME_MOUNT_SAINT_HELENS, REGION_CODE_WA, ASSOCIATION_CODE_W7W);

        assertEquals(summit1, summit2);
        assertEquals(summit1.hashCode(), summit2.hashCode());
    }

    @Test
    void shouldNotBeEqual_WhenReferencesDiffer() {
        Summit summit1 = new Summit(
                REFERENCE_W7W_LC_001, NAME_MOUNT_SAINT_HELENS, REGION_CODE_WA, ASSOCIATION_CODE_W7W);
        Summit summit2 = new Summit("W7W/LC-002", "Mount Adams", REGION_CODE_WA, ASSOCIATION_CODE_W7W);

        assertNotEquals(summit1, summit2);
    }

    @Test
    void shouldHandle_W7RegionSummits() {
        Summit summit = new Summit(
                REFERENCE_W7W_LC_001, NAME_MOUNT_SAINT_HELENS, REGION_CODE_WA, ASSOCIATION_CODE_W7W);

        assertEquals(REGION_CODE_WA, summit.regionCode());
        assertEquals(ASSOCIATION_CODE_W7W, summit.associationCode());
    }

    @Test
    void shouldHandle_W4RegionSummits() {
        Summit summit = new Summit("W4G/NG-001", "Brasstown Bald", "GA", "W4G");

        assertEquals("GA", summit.regionCode());
        assertEquals("W4G", summit.associationCode());
    }

    @Test
    void shouldHandle_W6RegionSummits() {
        Summit summit = new Summit("W6/CT-001", "Mount Whitney", "CA", "W6");

        assertEquals("CA", summit.regionCode());
        assertEquals("W6", summit.associationCode());
    }

    @Test
    void shouldHandle_InternationalSummits() {
        Summit summit = new Summit("G/LD-001", "Scafell Pike", null, "G/LD");

        assertNull(summit.regionCode());
        assertEquals("G/LD", summit.associationCode());
    }
}
