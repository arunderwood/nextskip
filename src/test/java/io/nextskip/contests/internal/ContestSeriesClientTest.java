package io.nextskip.contests.internal;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.nextskip.common.client.ExternalApiException;
import io.nextskip.common.model.FrequencyBand;
import io.nextskip.contests.internal.dto.ContestSeriesDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for ContestSeriesClient using WireMock.
 */
@SuppressWarnings("PMD.TooManyMethods") // Comprehensive test suite for scraper
class ContestSeriesClientTest {

    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_HTML = "text/html";
    private static final String TEST_CONTEST_NAME = "Test Contest";
    private static final String TD_CLOSE = "</td></tr>\n";

    private WireMockServer wireMockServer;
    private ContestSeriesClient client;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private RetryRegistry retryRegistry;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();

        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        retryRegistry = RetryRegistry.ofDefaults();

        String baseUrl = "http://localhost:" + wireMockServer.port();
        WebClient.Builder webClientBuilder = WebClient.builder();

        client = new StubContestSeriesClient(webClientBuilder, circuitBreakerRegistry,
                retryRegistry, baseUrl);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    // ========== fetchSeriesDetails tests ==========

    @Test
    void testFetchSeriesDetails_Success_ParsesAllFields() {
        String html = createContestDetailPage(
                "Indiana QSO Party",
                "160, 80, 40, 20, 15, 10m",
                "CW, SSB, Digital",
                "HDXA",
                "https://example.com/rules",
                "RS(T) + county/state/DX",
                "IN-QSO-PARTY",
                "November 1, 2025"
        );

        wireMockServer.stubFor(get(urlEqualTo("/contestdetails.php?ref=8"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_HTML)
                        .withBody(html)));

        ContestSeriesDto result = client.fetchSeriesDetails("8");

        assertNotNull(result);
        assertEquals("8", result.wa7bnmRef());
        assertEquals("Indiana QSO Party", result.name());
        assertEquals("HDXA", result.sponsor());
        assertEquals("https://example.com/rules", result.officialRulesUrl());
        assertEquals("RS(T) + county/state/DX", result.exchange());
        assertEquals("IN-QSO-PARTY", result.cabrilloName());
        assertEquals(LocalDate.of(2025, 11, 1), result.revisionDate());

        wireMockServer.verify(getRequestedFor(urlEqualTo("/contestdetails.php?ref=8")));
    }

    @Test
    void testFetchSeriesDetails_BandsParsing_Any() {
        String html = createContestDetailPage(TEST_CONTEST_NAME, "Any", "CW", null, null, null, null, null);

        wireMockServer.stubFor(get(urlEqualTo("/contestdetails.php?ref=1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_HTML)
                        .withBody(html)));

        ContestSeriesDto result = client.fetchSeriesDetails("1");

        // "Any" should include all HF bands
        assertTrue(result.bands().contains(FrequencyBand.BAND_160M));
        assertTrue(result.bands().contains(FrequencyBand.BAND_80M));
        assertTrue(result.bands().contains(FrequencyBand.BAND_40M));
        assertTrue(result.bands().contains(FrequencyBand.BAND_20M));
        assertTrue(result.bands().contains(FrequencyBand.BAND_15M));
        assertTrue(result.bands().contains(FrequencyBand.BAND_10M));
    }

    @Test
    void testFetchSeriesDetails_BandsParsing_ExceptWarc() {
        String html = createContestDetailPage(TEST_CONTEST_NAME, "Any except WARC", "CW", null, null, null, null, null);

        wireMockServer.stubFor(get(urlEqualTo("/contestdetails.php?ref=2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_HTML)
                        .withBody(html)));

        ContestSeriesDto result = client.fetchSeriesDetails("2");

        // Should have main contest bands but not WARC
        assertTrue(result.bands().contains(FrequencyBand.BAND_160M));
        assertTrue(result.bands().contains(FrequencyBand.BAND_80M));
        assertTrue(result.bands().contains(FrequencyBand.BAND_40M));
        assertTrue(result.bands().contains(FrequencyBand.BAND_20M));
        assertTrue(result.bands().contains(FrequencyBand.BAND_15M));
        assertTrue(result.bands().contains(FrequencyBand.BAND_10M));

        // WARC bands should be excluded
        assertFalse(result.bands().contains(FrequencyBand.BAND_60M));
        assertFalse(result.bands().contains(FrequencyBand.BAND_30M));
        assertFalse(result.bands().contains(FrequencyBand.BAND_17M));
        assertFalse(result.bands().contains(FrequencyBand.BAND_12M));
    }

    @Test
    void testFetchSeriesDetails_BandsParsing_SpecificBands() {
        String html = createContestDetailPage(TEST_CONTEST_NAME, "20, 40, 80m", "CW", null, null, null, null, null);

        wireMockServer.stubFor(get(urlEqualTo("/contestdetails.php?ref=3"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_HTML)
                        .withBody(html)));

        ContestSeriesDto result = client.fetchSeriesDetails("3");

        assertEquals(3, result.bands().size());
        assertTrue(result.bands().contains(FrequencyBand.BAND_20M));
        assertTrue(result.bands().contains(FrequencyBand.BAND_40M));
        assertTrue(result.bands().contains(FrequencyBand.BAND_80M));
    }

    @Test
    void testFetchSeriesDetails_ModesParsing_Any() {
        String html = createContestDetailPage(TEST_CONTEST_NAME, "40m", "Any", null, null, null, null, null);

        wireMockServer.stubFor(get(urlEqualTo("/contestdetails.php?ref=4"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_HTML)
                        .withBody(html)));

        ContestSeriesDto result = client.fetchSeriesDetails("4");

        assertTrue(result.modes().contains("CW"));
        assertTrue(result.modes().contains("SSB"));
        assertTrue(result.modes().contains("Digital"));
    }

    @Test
    void testFetchSeriesDetails_ModesParsing_Phone() {
        String html = createContestDetailPage(TEST_CONTEST_NAME, "40m", "Phone", null, null, null, null, null);

        wireMockServer.stubFor(get(urlEqualTo("/contestdetails.php?ref=5"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_HTML)
                        .withBody(html)));

        ContestSeriesDto result = client.fetchSeriesDetails("5");

        assertTrue(result.modes().contains("SSB"));
    }

    @Test
    void testFetchSeriesDetails_ModesParsing_Digital() {
        String html = createContestDetailPage(TEST_CONTEST_NAME, "40m", "RTTY, FT8", null, null, null, null, null);

        wireMockServer.stubFor(get(urlEqualTo("/contestdetails.php?ref=6"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_HTML)
                        .withBody(html)));

        ContestSeriesDto result = client.fetchSeriesDetails("6");

        assertTrue(result.modes().contains("Digital"));
    }

    @Test
    void testFetchSeriesDetails_ServerError_ThrowsException() {
        wireMockServer.stubFor(get(urlEqualTo("/contestdetails.php?ref=999"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        assertThrows(ExternalApiException.class, () -> client.fetchSeriesDetails("999"));
    }

    @Test
    void testFetchSeriesDetails_NotFound_ThrowsException() {
        wireMockServer.stubFor(get(urlEqualTo("/contestdetails.php?ref=404"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("Not Found")));

        assertThrows(ExternalApiException.class, () -> client.fetchSeriesDetails("404"));
    }

    @Test
    void testFetchSeriesDetails_EmptyResponse_ThrowsException() {
        wireMockServer.stubFor(get(urlEqualTo("/contestdetails.php?ref=empty"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_HTML)
                        .withBody("")));

        assertThrows(ExternalApiException.class, () -> client.fetchSeriesDetails("empty"));
    }

    @Test
    void testFetchSeriesDetails_MissingFields_ReturnsNulls() {
        // Minimal HTML with no contest details
        String html = "<html><head><title>" + TEST_CONTEST_NAME + "</title></head>"
                + "<body><h1>" + TEST_CONTEST_NAME + "</h1></body></html>";

        wireMockServer.stubFor(get(urlEqualTo("/contestdetails.php?ref=7"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_HTML)
                        .withBody(html)));

        ContestSeriesDto result = client.fetchSeriesDetails("7");

        assertNotNull(result);
        assertEquals("7", result.wa7bnmRef());
        assertEquals(TEST_CONTEST_NAME, result.name());
        assertTrue(result.bands().isEmpty());
        assertTrue(result.modes().isEmpty());
        assertNull(result.sponsor());
        assertNull(result.officialRulesUrl());
        assertNull(result.exchange());
        assertNull(result.cabrilloName());
        assertNull(result.revisionDate());
    }

    // ========== fetchRevisionDate tests ==========

    @Test
    void testFetchRevisionDate_Success() {
        String html = """
                <html>
                <body>
                Revision Date: November 1, 2025
                </body>
                </html>
                """;

        wireMockServer.stubFor(get(urlEqualTo("/contestdetails.php?ref=10"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_HTML)
                        .withBody(html)));

        Optional<LocalDate> result = client.fetchRevisionDate("10");

        assertTrue(result.isPresent());
        assertEquals(LocalDate.of(2025, 11, 1), result.get());
    }

    @Test
    void testFetchRevisionDate_NotFound_ReturnsEmpty() {
        String html = """
                <html>
                <body>
                No revision date here
                </body>
                </html>
                """;

        wireMockServer.stubFor(get(urlEqualTo("/contestdetails.php?ref=11"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_HTML)
                        .withBody(html)));

        Optional<LocalDate> result = client.fetchRevisionDate("11");

        assertTrue(result.isEmpty());
    }

    @Test
    void testFetchRevisionDate_ServerError_ReturnsEmpty() {
        wireMockServer.stubFor(get(urlEqualTo("/contestdetails.php?ref=12"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Error")));

        Optional<LocalDate> result = client.fetchRevisionDate("12");

        assertTrue(result.isEmpty());
    }

    // ========== parseRevisionDate tests ==========

    @Test
    void testParseRevisionDate_ValidFormat_Parses() {
        String html = "Some text\nRevision Date: December 25, 2025\nMore text";

        Optional<LocalDate> result = client.parseRevisionDate(html);

        assertTrue(result.isPresent());
        assertEquals(LocalDate.of(2025, 12, 25), result.get());
    }

    @Test
    void testParseRevisionDate_SingleDigitDay_Parses() {
        String html = "Revision Date: January 5, 2025";

        Optional<LocalDate> result = client.parseRevisionDate(html);

        assertTrue(result.isPresent());
        assertEquals(LocalDate.of(2025, 1, 5), result.get());
    }

    @Test
    void testParseRevisionDate_MissingDate_ReturnsEmpty() {
        String html = "No date information";

        Optional<LocalDate> result = client.parseRevisionDate(html);

        assertTrue(result.isEmpty());
    }

    @Test
    void testParseRevisionDate_InvalidFormat_ReturnsEmpty() {
        String html = "Revision Date: 2025-12-25";

        Optional<LocalDate> result = client.parseRevisionDate(html);

        assertTrue(result.isEmpty());
    }

    // ========== parseBands tests ==========

    @Test
    void testParseBands_AllFormat() {
        String html = createContestDetailPage("Test", "All HF bands", "CW", null, null, null, null, null);

        wireMockServer.stubFor(get(urlEqualTo("/contestdetails.php?ref=20"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_HTML)
                        .withBody(html)));

        ContestSeriesDto result = client.fetchSeriesDetails("20");

        // "All" should include all HF bands
        assertTrue(result.bands().size() >= 10);
    }

    // ========== Helper methods ==========

    /**
     * Creates a mock WA7BNM contest detail page HTML.
     */
    private String createContestDetailPage(
            String name,
            String bands,
            String modes,
            String sponsor,
            String rulesUrl,
            String exchange,
            String cabrilloName,
            String revisionDate) {

        StringBuilder html = new StringBuilder();
        html.append("<html>\n<head><title>").append(name)
                .append(" - Contest Calendar</title></head>\n<body>\n<h1>").append(name)
                .append("</h1>\n<table>\n");

        if (bands != null) {
            html.append("<tr><td><b>Bands:</b></td><td>").append(bands).append(TD_CLOSE);
        }
        if (modes != null) {
            html.append("<tr><td><b>Mode:</b></td><td>").append(modes).append(TD_CLOSE);
        }
        if (sponsor != null) {
            html.append("<tr><td><b>Sponsor:</b></td><td>").append(sponsor).append(TD_CLOSE);
        }
        if (rulesUrl != null) {
            html.append("<tr><td><b>Find rules at:</b></td><td><a href=\"").append(rulesUrl)
                    .append("\">Rules</a>").append(TD_CLOSE);
        }
        if (exchange != null) {
            html.append("<tr><td><b>Exchange:</b></td><td>").append(exchange).append(TD_CLOSE);
        }
        if (cabrilloName != null) {
            html.append("<tr><td><b>Cabrillo name:</b></td><td>").append(cabrilloName).append(TD_CLOSE);
        }

        html.append("</table>\n");

        if (revisionDate != null) {
            html.append("<p>Revision Date: ").append(revisionDate).append("</p>\n");
        }

        html.append("</body>\n</html>");
        return html.toString();
    }

    /**
     * Stub subclass that allows URL override for testing.
     */
    static class StubContestSeriesClient extends ContestSeriesClient {
        StubContestSeriesClient(
                WebClient.Builder webClientBuilder,
                CircuitBreakerRegistry circuitBreakerRegistry,
                RetryRegistry retryRegistry,
                String testUrl) {
            super(webClientBuilder, circuitBreakerRegistry, retryRegistry, testUrl);
        }
    }
}
