package io.nextskip.contests.internal.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for ContestICalDto validation.
 */
class ContestICalDtoTest {

    private static final String CONTEST_SUMMARY_REQUIRED = "Contest summary (name) is required";
    private static final String CONTEST = "Contest";

    @Test
    void shouldValidate_ValidDto() {
        Instant start = Instant.now();
        Instant end = start.plus(24, ChronoUnit.HOURS);

        ContestICalDto dto = new ContestICalDto(
                "Valid Contest",
                start,
                end,
                "https://example.com"
        );

        assertDoesNotThrow(dto::validate);
    }

    @Test
    void shouldValidate_ValidDto_WithoutUrl() {
        Instant start = Instant.now();
        Instant end = start.plus(24, ChronoUnit.HOURS);

        ContestICalDto dto = new ContestICalDto(
                "Valid Contest",
                start,
                end,
                null  // URL is optional
        );

        assertDoesNotThrow(dto::validate);
    }

    @Test
    void shouldValidate_NullSummary() {
        Instant start = Instant.now();
        Instant end = start.plus(24, ChronoUnit.HOURS);

        ContestICalDto dto = new ContestICalDto(null, start, end, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, dto::validate);
        assertEquals(CONTEST_SUMMARY_REQUIRED, ex.getMessage());
    }

    @Test
    void shouldValidate_BlankSummary() {
        Instant start = Instant.now();
        Instant end = start.plus(24, ChronoUnit.HOURS);

        ContestICalDto dto = new ContestICalDto("   ", start, end, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, dto::validate);
        assertEquals(CONTEST_SUMMARY_REQUIRED, ex.getMessage());
    }

    @Test
    void shouldValidate_EmptySummary() {
        Instant start = Instant.now();
        Instant end = start.plus(24, ChronoUnit.HOURS);

        ContestICalDto dto = new ContestICalDto("", start, end, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, dto::validate);
        assertEquals(CONTEST_SUMMARY_REQUIRED, ex.getMessage());
    }

    @Test
    void shouldValidate_NullStartTime() {
        Instant end = Instant.now().plus(24, ChronoUnit.HOURS);

        ContestICalDto dto = new ContestICalDto(CONTEST, null, end, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, dto::validate);
        assertEquals("Contest start time is required", ex.getMessage());
    }

    @Test
    void shouldValidate_NullEndTime() {
        Instant start = Instant.now();

        ContestICalDto dto = new ContestICalDto(CONTEST, start, null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, dto::validate);
        assertEquals("Contest end time is required", ex.getMessage());
    }

    @Test
    void shouldValidate_EndBeforeStart() {
        Instant start = Instant.now();
        Instant end = start.minus(1, ChronoUnit.HOURS);  // End before start

        ContestICalDto dto = new ContestICalDto(CONTEST, start, end, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, dto::validate);
        assertEquals("Contest end time must be after start time", ex.getMessage());
    }

    @Test
    void shouldValidate_EndEqualsStart() {
        Instant time = Instant.now();

        ContestICalDto dto = new ContestICalDto(CONTEST, time, time, null);

        // End time equals start time is technically valid (zero duration event)
        // The check is specifically for end BEFORE start
        assertDoesNotThrow(dto::validate);
    }

    @Test
    void shouldAccess_RecordAccessors() {
        Instant start = Instant.now();
        Instant end = start.plus(24, ChronoUnit.HOURS);
        String url = "https://example.com/contest";

        ContestICalDto dto = new ContestICalDto("Test Contest", start, end, url);

        assertEquals("Test Contest", dto.summary());
        assertEquals(start, dto.startTime());
        assertEquals(end, dto.endTime());
        assertEquals(url, dto.detailsUrl());
    }
}
