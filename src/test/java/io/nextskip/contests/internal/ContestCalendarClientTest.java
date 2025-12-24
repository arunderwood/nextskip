package io.nextskip.contests.internal;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.nextskip.contests.internal.dto.ContestICalDto;
import io.nextskip.propagation.internal.ExternalApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for ContestCalendarClient using WireMock.
 */
class ContestCalendarClientTest {

    private WireMockServer wireMockServer;
    private ContestCalendarClient client;
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();

        String baseUrl = "http://localhost:" + wireMockServer.port();
        WebClient.Builder webClientBuilder = WebClient.builder();
        cacheManager = new ConcurrentMapCacheManager("contests");

        client = new TestContestCalendarClient(webClientBuilder, cacheManager, baseUrl);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void testFetch_Success() {
        String icalResponse = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//WA7BNM//Contest Calendar//EN
            BEGIN:VEVENT
            SUMMARY:ARRL 10-Meter Contest
            DTSTART:20251214T000000Z
            DTEND:20251215T235959Z
            URL:https://www.contestcalendar.com/contestdetails.php?ref=123
            END:VEVENT
            BEGIN:VEVENT
            SUMMARY:CQ WW DX Contest
            DTSTART:20251128T000000Z
            DTEND:20251130T000000Z
            URL:https://www.contestcalendar.com/contestdetails.php?ref=456
            END:VEVENT
            END:VCALENDAR
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/calendar")
                        .withBody(icalResponse)));

        List<ContestICalDto> result = client.fetch();

        assertNotNull(result);
        assertEquals(2, result.size());

        // Verify first contest
        ContestICalDto contest1 = result.stream()
                .filter(c -> c.summary().contains("10-Meter"))
                .findFirst()
                .orElseThrow();
        assertEquals("ARRL 10-Meter Contest", contest1.summary());
        assertNotNull(contest1.startTime());
        assertNotNull(contest1.endTime());
        assertTrue(contest1.detailsUrl().contains("contestcalendar.com"));

        wireMockServer.verify(getRequestedFor(urlEqualTo("/")));
    }

    @Test
    void testFetch_SingleContest() {
        String icalResponse = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//WA7BNM//Contest Calendar//EN
            BEGIN:VEVENT
            SUMMARY:NRAU-Baltic Contest
            DTSTART:20251219T180000Z
            DTEND:20251219T210000Z
            URL:https://www.contestcalendar.com/contestdetails.php?ref=789
            END:VEVENT
            END:VCALENDAR
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/calendar")
                        .withBody(icalResponse)));

        List<ContestICalDto> result = client.fetch();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("NRAU-Baltic Contest", result.get(0).summary());
    }

    @Test
    void testFetch_EmptyCalendar() {
        String icalResponse = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//WA7BNM//Contest Calendar//EN
            END:VCALENDAR
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/calendar")
                        .withBody(icalResponse)));

        List<ContestICalDto> result = client.fetch();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFetch_MalformedEvent_Skipped() {
        // One valid event and one malformed (missing required fields)
        String icalResponse = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//WA7BNM//Contest Calendar//EN
            BEGIN:VEVENT
            SUMMARY:Valid Contest
            DTSTART:20251214T000000Z
            DTEND:20251215T235959Z
            END:VEVENT
            BEGIN:VEVENT
            SUMMARY:
            DTSTART:20251214T000000Z
            DTEND:20251215T235959Z
            END:VEVENT
            END:VCALENDAR
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/calendar")
                        .withBody(icalResponse)));

        List<ContestICalDto> result = client.fetch();

        // Only the valid contest should be returned
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Valid Contest", result.get(0).summary());
    }

    @Test
    void testFetch_ServerError() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        assertThrows(ExternalApiException.class, () -> client.fetch());
    }

    @Test
    void testFetch_EmptyResponse() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/calendar")
                        .withBody("")));

        assertThrows(ExternalApiException.class, () -> client.fetch());
    }

    @Test
    void testFetch_NotFoundError() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("Not Found")));

        assertThrows(ExternalApiException.class, () -> client.fetch());
    }

    @Test
    void testGetSourceName() {
        assertEquals("WA7BNM Contest Calendar", client.getSourceName());
    }

    @Test
    void testFetch_ContestWithoutUrl() {
        String icalResponse = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//WA7BNM//Contest Calendar//EN
            BEGIN:VEVENT
            SUMMARY:Mystery Contest
            DTSTART:20251220T000000Z
            DTEND:20251221T000000Z
            END:VEVENT
            END:VCALENDAR
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/calendar")
                        .withBody(icalResponse)));

        List<ContestICalDto> result = client.fetch();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Mystery Contest", result.get(0).summary());
        // URL is optional - should still parse successfully
    }

    @Test
    void testFetch_TimezoneHandling() {
        // Test with UTC timezone (common format from WA7BNM)
        String icalResponse = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//WA7BNM//Contest Calendar//EN
            BEGIN:VEVENT
            SUMMARY:Timezone Test Contest
            DTSTART:20251225T120000Z
            DTEND:20251225T180000Z
            URL:https://www.contestcalendar.com/test
            END:VEVENT
            END:VCALENDAR
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/calendar")
                        .withBody(icalResponse)));

        List<ContestICalDto> result = client.fetch();

        assertNotNull(result);
        assertEquals(1, result.size());

        ContestICalDto contest = result.get(0);
        assertNotNull(contest.startTime());
        assertNotNull(contest.endTime());
        // End time should be after start time
        assertTrue(contest.endTime().isAfter(contest.startTime()));
    }

    /**
     * Test subclass that allows URL override for testing.
     */
    static class TestContestCalendarClient extends ContestCalendarClient {
        TestContestCalendarClient(WebClient.Builder webClientBuilder, CacheManager cacheManager, String testUrl) {
            super(webClientBuilder, cacheManager, testUrl);
        }
    }
}
