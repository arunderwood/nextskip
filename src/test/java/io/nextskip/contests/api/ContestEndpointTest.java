package io.nextskip.contests.api;

import io.nextskip.contests.model.Contest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ContestEndpoint.
 *
 * <p>Since the endpoint is now a thin delegate to the service layer,
 * these tests verify that the endpoint correctly delegates to the service
 * and returns the service's response unchanged.
 */
@ExtendWith(MockitoExtension.class)
class ContestEndpointTest {

    @Mock
    private ContestService contestService;

    private ContestEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new ContestEndpoint(contestService);
    }

    @Test
    void testGetContests_DelegatesToService() {
        // Given: Service returns response with contests
        Instant now = Instant.now();
        List<Contest> contests = List.of(
                createContest("ARRL 10-Meter Contest", now.plus(1, ChronoUnit.HOURS)),
                createContest("CQ WW DX CW", now.plus(48, ChronoUnit.HOURS))
        );
        ContestsResponse serviceResponse = new ContestsResponse(
                contests, 0, 1, 2, now);

        when(contestService.getContestsResponse()).thenReturn(serviceResponse);

        // When
        ContestsResponse response = endpoint.getContests();

        // Then: Should return service response unchanged
        assertNotNull(response);
        assertEquals(2, response.contests().size());
        assertEquals(0, response.activeCount());
        assertEquals(1, response.upcomingCount());
        assertEquals(2, response.totalCount());
        assertEquals(now, response.lastUpdated());

        verify(contestService).getContestsResponse();
    }

    @Test
    void testGetContests_EmptyList() {
        // Given: Service returns empty response
        Instant now = Instant.now();
        ContestsResponse serviceResponse = new ContestsResponse(
                List.of(), 0, 0, 0, now);

        when(contestService.getContestsResponse()).thenReturn(serviceResponse);

        // When
        ContestsResponse response = endpoint.getContests();

        // Then
        assertNotNull(response);
        assertTrue(response.contests().isEmpty());
        assertEquals(0, response.totalCount());

        verify(contestService).getContestsResponse();
    }

    @Test
    void testGetContests_WithActiveContests() {
        // Given: Service returns response with active contests
        Instant now = Instant.now();
        List<Contest> contests = List.of(
                createContest("Active Contest", now.minus(1, ChronoUnit.HOURS))
        );
        ContestsResponse serviceResponse = new ContestsResponse(
                contests, 1, 0, 1, now);

        when(contestService.getContestsResponse()).thenReturn(serviceResponse);

        // When
        ContestsResponse response = endpoint.getContests();

        // Then
        assertNotNull(response);
        assertEquals(1, response.activeCount());
        assertEquals(0, response.upcomingCount());

        verify(contestService).getContestsResponse();
    }

    /**
     * Helper method to create a test contest.
     */
    private Contest createContest(String name, Instant startTime) {
        return new Contest(
                name,
                startTime,
                startTime.plus(48, ChronoUnit.HOURS),
                Set.of(),
                Set.of(),
                null,
                "https://contestcalendar.com/test",
                null
        );
    }
}
