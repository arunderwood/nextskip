package io.nextskip.contests.internal;

import io.nextskip.contests.internal.dto.ContestICalDto;
import io.nextskip.contests.model.Contest;
import io.nextskip.propagation.internal.ExternalApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ContestServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class ContestServiceImplTest {

    @Mock
    private ContestCalendarClient calendarClient;

    private ContestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ContestServiceImpl(calendarClient);
    }

    @Test
    void testGetUpcomingContests_Success() {
        Instant now = Instant.now();
        List<ContestICalDto> dtos = List.of(
                new ContestICalDto(
                        "ARRL 10-Meter Contest",
                        now.plus(24, ChronoUnit.HOURS),
                        now.plus(48, ChronoUnit.HOURS),
                        "https://contestcalendar.com/arrl-10m"
                ),
                new ContestICalDto(
                        "CQ WW DX CW",
                        now.plus(72, ChronoUnit.HOURS),
                        now.plus(120, ChronoUnit.HOURS),
                        "https://contestcalendar.com/cqww"
                )
        );

        when(calendarClient.fetch()).thenReturn(dtos);

        List<Contest> result = service.getUpcomingContests();

        assertNotNull(result);
        assertEquals(2, result.size());

        // Verify first contest
        Contest contest1 = result.stream()
                .filter(c -> c.name().contains("10-Meter"))
                .findFirst()
                .orElseThrow();
        assertEquals("ARRL 10-Meter Contest", contest1.name());
        assertNotNull(contest1.startTime());
        assertNotNull(contest1.endTime());
        assertEquals("https://contestcalendar.com/arrl-10m", contest1.calendarSourceUrl());

        // Bands, modes, sponsor, officialRulesUrl should be empty/null (not available from iCal)
        assertTrue(contest1.bands().isEmpty());
        assertTrue(contest1.modes().isEmpty());

        verify(calendarClient).fetch();
    }

    @Test
    void testGetUpcomingContests_Empty() {
        when(calendarClient.fetch()).thenReturn(List.of());

        List<Contest> result = service.getUpcomingContests();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(calendarClient).fetch();
    }

    @Test
    void testGetUpcomingContests_ClientException() {
        when(calendarClient.fetch()).thenThrow(new ExternalApiException("WA7BNM", "Connection failed"));

        List<Contest> result = service.getUpcomingContests();

        // Service should catch exception and return empty list (graceful degradation)
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(calendarClient).fetch();
    }

    @Test
    void testGetUpcomingContests_RuntimeException() {
        when(calendarClient.fetch()).thenThrow(new RuntimeException("Unexpected error"));

        List<Contest> result = service.getUpcomingContests();

        // Service should catch exception and return empty list (graceful degradation)
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(calendarClient).fetch();
    }

    @Test
    void testGetUpcomingContests_ContestConversion() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(25, ChronoUnit.HOURS);

        List<ContestICalDto> dtos = List.of(
                new ContestICalDto(
                        "Test Contest",
                        start,
                        end,
                        "https://example.com/contest"
                )
        );

        when(calendarClient.fetch()).thenReturn(dtos);

        List<Contest> result = service.getUpcomingContests();

        assertNotNull(result);
        assertEquals(1, result.size());

        Contest contest = result.get(0);
        assertEquals("Test Contest", contest.name());
        assertEquals(start, contest.startTime());
        assertEquals(end, contest.endTime());
        assertEquals("https://example.com/contest", contest.calendarSourceUrl());

        // Verify default values for unavailable iCal fields
        assertNotNull(contest.bands());
        assertTrue(contest.bands().isEmpty());
        assertNotNull(contest.modes());
        assertTrue(contest.modes().isEmpty());
    }

    @Test
    void testGetUpcomingContests_MultipleContests() {
        Instant now = Instant.now();
        List<ContestICalDto> dtos = List.of(
                new ContestICalDto("Contest 1", now.plus(1, ChronoUnit.HOURS), now.plus(25, ChronoUnit.HOURS), null),
                new ContestICalDto("Contest 2", now.plus(48, ChronoUnit.HOURS), now.plus(72, ChronoUnit.HOURS), null),
                new ContestICalDto("Contest 3", now.plus(96, ChronoUnit.HOURS), now.plus(120, ChronoUnit.HOURS), null)
        );

        when(calendarClient.fetch()).thenReturn(dtos);

        List<Contest> result = service.getUpcomingContests();

        assertEquals(3, result.size());

        // All contests should be converted
        assertTrue(result.stream().anyMatch(c -> "Contest 1".equals(c.name())));
        assertTrue(result.stream().anyMatch(c -> "Contest 2".equals(c.name())));
        assertTrue(result.stream().anyMatch(c -> "Contest 3".equals(c.name())));
    }

    @Test
    void testGetUpcomingContests_NullDetailsUrl() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(24, ChronoUnit.HOURS);

        List<ContestICalDto> dtos = List.of(
                new ContestICalDto("Contest Without URL", start, end, null)
        );

        when(calendarClient.fetch()).thenReturn(dtos);

        List<Contest> result = service.getUpcomingContests();

        assertNotNull(result);
        assertEquals(1, result.size());

        // Should handle null URL gracefully
        Contest contest = result.get(0);
        assertEquals("Contest Without URL", contest.name());
        // calendarSourceUrl will be null
    }
}
