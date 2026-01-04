package io.nextskip.activations.internal.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SotaSpotDto record.
 *
 * Tests JSON deserialization and field mapping.
 */
class SotaSpotDtoTest {

    private static final String TIMESTAMP = "2025-12-14T14:30:00";
    private static final long SPOT_ID = 123456L;
    private static final String K2DEF_PORTABLE = "K2DEF/P";
    private static final String SUMMIT_CODE = "W7W/LC-001";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserialize_fromJson() throws Exception {
        // Given: SOTA API JSON response
        String json = """
            {
              "id": 123456,
              "activatorCallsign": "K2DEF/P",
              "associationCode": "W7W",
              "summitCode": "W7W/LC-001",
              "frequency": "14.062",
              "mode": "CW",
              "summitDetails": "Mount Test",
              "timeStamp": "2025-12-14T14:30:00",
              "comments": "QRV now",
              "callsign": "K2DEF",
              "activatorName": "John Doe",
              "highlander": false
            }
            """;

        // When: Deserialize
        SotaSpotDto dto = objectMapper.readValue(json, SotaSpotDto.class);

        // Then: All fields mapped correctly
        assertEquals(SPOT_ID, dto.id());
        assertEquals(K2DEF_PORTABLE, dto.activatorCallsign());
        assertEquals("W7W", dto.associationCode());
        assertEquals(SUMMIT_CODE, dto.summitCode());
        assertEquals("14.062", dto.frequency());
        assertEquals("CW", dto.mode());
        assertEquals("Mount Test", dto.summitDetails());
        assertEquals(TIMESTAMP, dto.timeStamp());
        assertEquals("QRV now", dto.comments());
        assertEquals("K2DEF", dto.callsign());
        assertEquals("John Doe", dto.activatorName());
        assertFalse(dto.highlander());
    }

    @Test
    void shouldCreateDto_withConstructor() {
        // When: Create DTO directly
        SotaSpotDto dto = new SotaSpotDto(
                SPOT_ID,
                K2DEF_PORTABLE,
                "W7W",
                SUMMIT_CODE,
                "14.062",
                "CW",
                "Mount Test",
                TIMESTAMP,
                "QRV now",
                "K2DEF",
                "John Doe",
                false
        );

        // Then: All fields accessible
        assertEquals(SPOT_ID, dto.id());
        assertEquals(K2DEF_PORTABLE, dto.activatorCallsign());
        assertEquals(SUMMIT_CODE, dto.summitCode());
    }

    @Test
    void shouldHandle_highlanderFlag() throws Exception {
        // Given: SOTA spot with highlander = true
        String json = """
            {
              "id": 123456,
              "activatorCallsign": "K2DEF/P",
              "summitCode": "W7W/LC-001",
              "timeStamp": "2025-12-14T14:30:00",
              "highlander": true
            }
            """;

        // When: Deserialize
        SotaSpotDto dto = objectMapper.readValue(json, SotaSpotDto.class);

        // Then: Highlander flag is true
        assertTrue(dto.highlander());
    }
}
