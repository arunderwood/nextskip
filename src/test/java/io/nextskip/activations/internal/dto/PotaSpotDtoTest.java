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

    private static final String TIMESTAMP = "2025-12-14T12:30:00";
    private static final long SPOT_ID = 12345L;
    private static final String CALLSIGN_W1ABC = "W1ABC";
    private static final String PARK_REFERENCE = "US-0001";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserialize_fromJson() throws Exception {
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
        assertEquals(SPOT_ID, dto.spotId());
        assertEquals(CALLSIGN_W1ABC, dto.activator());
        assertEquals(PARK_REFERENCE, dto.reference());
        assertEquals("14.250", dto.frequency());
        assertEquals("SSB", dto.mode());
        assertEquals("Test Park", dto.name());
        assertEquals("FN42", dto.grid6());
        assertEquals("42.5", dto.latitude());
        assertEquals("-71.3", dto.longitude());
        assertEquals(TIMESTAMP, dto.spotTime());
        assertEquals(15, dto.qsos());
        assertEquals("K2DEF", dto.spotter());
        assertEquals("Good signal", dto.comments());
    }

    @Test
    void shouldCreate_dtoWithConstructor() {
        // When: Create DTO directly
        PotaSpotDto dto = new PotaSpotDto(
                SPOT_ID,
                "W1ABC",
                "US-0001",
                "14.250",
                "SSB",
                "Test Park",
                "US-CO",  // locationDesc
                "FN42",
                "42.5",
                "-71.3",
                TIMESTAMP,
                15,
                "K2DEF",
                "Good signal"
        );

        // Then: All fields accessible
        assertEquals(SPOT_ID, dto.spotId());
        assertEquals(CALLSIGN_W1ABC, dto.activator());
        assertEquals(PARK_REFERENCE, dto.reference());
    }
}
