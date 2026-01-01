package io.nextskip.contests.internal;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.contests.model.Contest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ContestServiceImpl.
 *
 * Tests the service layer that reads contest data from the LoadingCache.
 */
@ExtendWith(MockitoExtension.class)
class ContestServiceImplTest {

    private static final Instant FIXED_TIME = Instant.parse("2025-01-15T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_TIME, ZoneOffset.UTC);

    @Mock
    private LoadingCache<String, List<Contest>> contestsCache;

    private ContestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ContestServiceImpl(contestsCache, FIXED_CLOCK);
    }

    @Test
    void testGetUpcomingContests_Success() {
        Instant now = Instant.now();
        List<Contest> contests = List.of(
                createContest("ARRL 10-Meter Contest",
                        now.plus(24, ChronoUnit.HOURS),
                        now.plus(48, ChronoUnit.HOURS)),
                createContest("CQ WW DX CW",
                        now.plus(72, ChronoUnit.HOURS),
                        now.plus(120, ChronoUnit.HOURS))
        );

        when(contestsCache.get(CacheConfig.CACHE_KEY)).thenReturn(contests);

        List<Contest> result = service.getUpcomingContests();

        assertNotNull(result);
        assertEquals(2, result.size());

        Contest contest1 = result.stream()
                .filter(c -> c.name().contains("10-Meter"))
                .findFirst()
                .orElseThrow();
        assertEquals("ARRL 10-Meter Contest", contest1.name());
        assertNotNull(contest1.startTime());
        assertNotNull(contest1.endTime());

        verify(contestsCache).get(CacheConfig.CACHE_KEY);
    }

    @Test
    void testGetUpcomingContests_Empty() {
        when(contestsCache.get(CacheConfig.CACHE_KEY)).thenReturn(List.of());

        List<Contest> result = service.getUpcomingContests();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(contestsCache).get(CacheConfig.CACHE_KEY);
    }

    @Test
    void testGetUpcomingContests_NullFromCache() {
        when(contestsCache.get(CacheConfig.CACHE_KEY)).thenReturn(null);

        List<Contest> result = service.getUpcomingContests();

        // Should handle null gracefully
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(contestsCache).get(CacheConfig.CACHE_KEY);
    }

    @Test
    void testGetUpcomingContests_MultipleContests() {
        Instant now = Instant.now();
        List<Contest> contests = List.of(
                createContest("Contest 1", now.plus(1, ChronoUnit.HOURS), now.plus(25, ChronoUnit.HOURS)),
                createContest("Contest 2", now.plus(48, ChronoUnit.HOURS), now.plus(72, ChronoUnit.HOURS)),
                createContest("Contest 3", now.plus(96, ChronoUnit.HOURS), now.plus(120, ChronoUnit.HOURS))
        );

        when(contestsCache.get(CacheConfig.CACHE_KEY)).thenReturn(contests);

        List<Contest> result = service.getUpcomingContests();

        assertEquals(3, result.size());

        // All contests should be present
        assertTrue(result.stream().anyMatch(c -> "Contest 1".equals(c.name())));
        assertTrue(result.stream().anyMatch(c -> "Contest 2".equals(c.name())));
        assertTrue(result.stream().anyMatch(c -> "Contest 3".equals(c.name())));
    }

    // ========== getContestsResponse() tests ==========

    @Test
    void testGetContestsResponse_CountsActiveContests() {
        Instant now = Instant.now();
        List<Contest> contests = List.of(
                // Active contest (started 1 hour ago, ends in 23 hours)
                createContest("Active Contest 1",
                        now.minus(1, ChronoUnit.HOURS),
                        now.plus(23, ChronoUnit.HOURS)),
                // Active contest (started 12 hours ago, ends in 12 hours)
                createContest("Active Contest 2",
                        now.minus(12, ChronoUnit.HOURS),
                        now.plus(12, ChronoUnit.HOURS)),
                // Upcoming contest (starts in 2 hours)
                createContest("Upcoming Contest",
                        now.plus(2, ChronoUnit.HOURS),
                        now.plus(26, ChronoUnit.HOURS))
        );

        when(contestsCache.get(CacheConfig.CACHE_KEY)).thenReturn(contests);

        io.nextskip.contests.api.ContestsResponse response = service.getContestsResponse();

        assertNotNull(response);
        assertEquals(2, response.activeCount(), "Should count 2 active contests");
        assertEquals(3, response.totalCount(), "Should have 3 total contests");
    }

    @Test
    void testGetContestsResponse_CountsUpcomingWithin24Hours() {
        Instant now = Instant.now();
        List<Contest> contests = List.of(
                // Upcoming within 24 hours (starts in 2 hours)
                createContest("Upcoming Soon 1",
                        now.plus(2, ChronoUnit.HOURS),
                        now.plus(26, ChronoUnit.HOURS)),
                // Upcoming within 24 hours (starts in 12 hours)
                createContest("Upcoming Soon 2",
                        now.plus(12, ChronoUnit.HOURS),
                        now.plus(36, ChronoUnit.HOURS)),
                // Upcoming but beyond 24 hours (starts in 48 hours)
                createContest("Upcoming Later",
                        now.plus(48, ChronoUnit.HOURS),
                        now.plus(72, ChronoUnit.HOURS)),
                // Active contest (should not count as upcoming)
                createContest("Active Contest",
                        now.minus(1, ChronoUnit.HOURS),
                        now.plus(23, ChronoUnit.HOURS))
        );

        when(contestsCache.get(CacheConfig.CACHE_KEY)).thenReturn(contests);

        io.nextskip.contests.api.ContestsResponse response = service.getContestsResponse();

        assertNotNull(response);
        assertEquals(2, response.upcomingCount(), "Should count 2 upcoming contests within 24 hours");
        assertEquals(1, response.activeCount(), "Should count 1 active contest");
        assertEquals(4, response.totalCount(), "Should have 4 total contests");
    }

    @Test
    void testGetContestsResponse_CalculatesTotalCount() {
        Instant now = Instant.now();
        List<Contest> contests = List.of(
                createContest("Contest 1", now.plus(1, ChronoUnit.HOURS), now.plus(25, ChronoUnit.HOURS)),
                createContest("Contest 2", now.plus(48, ChronoUnit.HOURS), now.plus(72, ChronoUnit.HOURS)),
                createContest("Contest 3", now.plus(96, ChronoUnit.HOURS), now.plus(120, ChronoUnit.HOURS)),
                createContest("Contest 4", now.plus(144, ChronoUnit.HOURS), now.plus(168, ChronoUnit.HOURS)),
                createContest("Contest 5", now.plus(192, ChronoUnit.HOURS), now.plus(216, ChronoUnit.HOURS))
        );

        when(contestsCache.get(CacheConfig.CACHE_KEY)).thenReturn(contests);

        io.nextskip.contests.api.ContestsResponse response = service.getContestsResponse();

        assertNotNull(response);
        assertEquals(5, response.totalCount(), "Total should match contest list size");
        assertEquals(5, response.contests().size(), "Contests list should have 5 items");
    }

    @Test
    void testGetContestsResponse_SetsTimestamp() {
        // Given: Fixed clock is injected
        when(contestsCache.get(CacheConfig.CACHE_KEY)).thenReturn(List.of());

        // When
        io.nextskip.contests.api.ContestsResponse response = service.getContestsResponse();

        // Then: Timestamp should match the fixed clock time exactly
        assertNotNull(response);
        assertEquals(FIXED_TIME, response.lastUpdated(), "lastUpdated should match fixed clock time");
    }

    /**
     * Helper method to create a test Contest.
     */
    private Contest createContest(String name, Instant start, Instant end) {
        return new Contest(
                name,
                start,
                end,
                Set.of(),
                Set.of(),
                null,   // sponsor
                null,   // officialRulesUrl
                "https://example.com/contest"  // calendarSourceUrl
        );
    }
}
