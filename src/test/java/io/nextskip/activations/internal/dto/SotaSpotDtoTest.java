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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeFromJson() throws Exception {
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
        assertEquals(123456L, dto.id());
        assertEquals("K2DEF/P", dto.activatorCallsign());
        assertEquals("W7W", dto.associationCode());
        assertEquals("W7W/LC-001", dto.summitCode());
        assertEquals("14.062", dto.frequency());
        assertEquals("CW", dto.mode());
        assertEquals("Mount Test", dto.summitDetails());
        assertEquals("2025-12-14T14:30:00", dto.timeStamp());
        assertEquals("QRV now", dto.comments());
        assertEquals("K2DEF", dto.callsign());
        assertEquals("John Doe", dto.activatorName());
        assertFalse(dto.highlander());
    }

    @Test
    void shouldHandleNullFields() throws Exception {
        // Given: Minimal JSON with null fields
        String json = """
            {
              "id": 123456,
              "activatorCallsign": "K2DEF/P",
              "summitCode": "W7W/LC-001",
              "timeStamp": "2025-12-14T14:30:00",
              "frequency": null,
              "mode": null,
              "summitDetails": null,
              "comments": null,
              "associationCode": null,
              "callsign": null,
              "activatorName": null,
              "highlander": null
            }
            """;

        // When: Deserialize
        SotaSpotDto dto = objectMapper.readValue(json, SotaSpotDto.class);

        // Then: Required fields present, optional fields null
        assertEquals(123456L, dto.id());
        assertEquals("K2DEF/P", dto.activatorCallsign());
        assertEquals("W7W/LC-001", dto.summitCode());
        assertEquals("2025-12-14T14:30:00", dto.timeStamp());
        assertNull(dto.frequency());
        assertNull(dto.mode());
        assertNull(dto.summitDetails());
        assertNull(dto.comments());
        assertNull(dto.associationCode());
        assertNull(dto.callsign());
        assertNull(dto.activatorName());
        assertNull(dto.highlander());
    }

    @Test
    void shouldIgnoreUnknownFields() throws Exception {
        // Given: JSON with extra unknown fields
        String json = """
            {
              "id": 123456,
              "activatorCallsign": "K2DEF/P",
              "summitCode": "W7W/LC-001",
              "timeStamp": "2025-12-14T14:30:00",
              "unknownField1": "value1",
              "futureField": 999
            }
            """;

        // When: Deserialize (should not throw)
        SotaSpotDto dto = objectMapper.readValue(json, SotaSpotDto.class);

        // Then: Known fields mapped, unknown fields ignored
        assertNotNull(dto);
        assertEquals(123456L, dto.id());
        assertEquals("K2DEF/P", dto.activatorCallsign());
    }

    @Test
    void shouldCreateDtoWithConstructor() {
        // When: Create DTO directly
        SotaSpotDto dto = new SotaSpotDto(
                123456L,
                "K2DEF/P",
                "W7W",
                "W7W/LC-001",
                "14.062",
                "CW",
                "Mount Test",
                "2025-12-14T14:30:00",
                "QRV now",
                "K2DEF",
                "John Doe",
                false
        );

        // Then: All fields accessible
        assertEquals(123456L, dto.id());
        assertEquals("K2DEF/P", dto.activatorCallsign());
        assertEquals("W7W/LC-001", dto.summitCode());
    }

    @Test
    void shouldHandleHighlanderFlag() throws Exception {
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
