package io.nextskip.activations.internal.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PotaSpotDto record.
 *
 * Tests JSON deserialization and field mapping.
 */
class PotaSpotDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeFromJson() throws Exception {
        // Given: POTA API JSON response
        String json = """
            {
              "spotId": 12345,
              "activator": "W1ABC",
              "reference": "US-0001",
              "frequency": "14.250",
              "mode": "SSB",
              "name": "Test Park",
              "grid6": "FN42",
              "latitude": "42.5",
              "longitude": "-71.3",
              "spotTime": "2025-12-14T12:30:00",
              "qsos": 15,
              "spotter": "K2DEF",
              "comments": "Good signal"
            }
            """;

        // When: Deserialize
        PotaSpotDto dto = objectMapper.readValue(json, PotaSpotDto.class);

        // Then: All fields mapped correctly
        assertEquals(12345L, dto.spotId());
        assertEquals("W1ABC", dto.activator());
        assertEquals("US-0001", dto.reference());
        assertEquals("14.250", dto.frequency());
        assertEquals("SSB", dto.mode());
        assertEquals("Test Park", dto.name());
        assertEquals("FN42", dto.grid6());
        assertEquals("42.5", dto.latitude());
        assertEquals("-71.3", dto.longitude());
        assertEquals("2025-12-14T12:30:00", dto.spotTime());
        assertEquals(15, dto.qsos());
        assertEquals("K2DEF", dto.spotter());
        assertEquals("Good signal", dto.comments());
    }

    @Test
    void shouldHandleNullFields() throws Exception {
        // Given: Minimal JSON with null fields
        String json = """
            {
              "spotId": 12345,
              "activator": "W1ABC",
              "reference": "US-0001",
              "frequency": null,
              "mode": null,
              "name": null,
              "grid6": null,
              "latitude": null,
              "longitude": null,
              "spotTime": "2025-12-14T12:30:00",
              "qsos": null,
              "spotter": null,
              "comments": null
            }
            """;

        // When: Deserialize
        PotaSpotDto dto = objectMapper.readValue(json, PotaSpotDto.class);

        // Then: Required fields present, optional fields null
        assertEquals(12345L, dto.spotId());
        assertEquals("W1ABC", dto.activator());
        assertEquals("US-0001", dto.reference());
        assertNull(dto.frequency());
        assertNull(dto.mode());
        assertNull(dto.name());
        assertNull(dto.grid6());
        assertNull(dto.latitude());
        assertNull(dto.longitude());
        assertEquals("2025-12-14T12:30:00", dto.spotTime());
        assertNull(dto.qsos());
        assertNull(dto.spotter());
        assertNull(dto.comments());
    }

    @Test
    void shouldIgnoreUnknownFields() throws Exception {
        // Given: JSON with extra unknown fields
        String json = """
            {
              "spotId": 12345,
              "activator": "W1ABC",
              "reference": "US-0001",
              "spotTime": "2025-12-14T12:30:00",
              "unknownField1": "value1",
              "unknownField2": 999
            }
            """;

        // When: Deserialize (should not throw)
        PotaSpotDto dto = objectMapper.readValue(json, PotaSpotDto.class);

        // Then: Known fields mapped, unknown fields ignored
        assertNotNull(dto);
        assertEquals(12345L, dto.spotId());
        assertEquals("W1ABC", dto.activator());
    }

    @Test
    void shouldCreateDtoWithConstructor() {
        // When: Create DTO directly
        PotaSpotDto dto = new PotaSpotDto(
                12345L,
                "W1ABC",
                "US-0001",
                "14.250",
                "SSB",
                "Test Park",
                "FN42",
                "42.5",
                "-71.3",
                "2025-12-14T12:30:00",
                15,
                "K2DEF",
                "Good signal"
        );

        // Then: All fields accessible
        assertEquals(12345L, dto.spotId());
        assertEquals("W1ABC", dto.activator());
        assertEquals("US-0001", dto.reference());
    }
}
