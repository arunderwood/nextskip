package io.nextskip.propagation.internal;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.nextskip.common.client.ExternalApiException;
import io.nextskip.common.client.InvalidApiResponseException;
import io.nextskip.common.model.FrequencyBand;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.BandConditionRating;
import io.nextskip.propagation.model.SolarIndices;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

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
 * Unit tests for unified HamQslClient using WireMock.
 *
 * <p>Tests that the unified client correctly extracts both solar indices
 * and band conditions from a single HTTP request.
 */
@SuppressWarnings("PMD.TooManyMethods") // Comprehensive test suite
class HamQslClientTest {

    private static final String HAMQSL_SOURCE = "HamQSL";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_XML = "text/xml";

    private WireMockServer wireMockServer;
    private HamQslClient client;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private RetryRegistry retryRegistry;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();

        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        retryRegistry = RetryRegistry.ofDefaults();

        String baseUrl = wireMockServer.baseUrl();
        WebClient.Builder webClientBuilder = WebClient.builder();

        client = new StubHamQslClient(webClientBuilder,
                circuitBreakerRegistry, retryRegistry, baseUrl);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    // ========== Solar Indices Tests ==========

    @Test
    void testFetch_ValidResponse_ExtractsSolarIndices() {
        String xmlResponse = createValidXmlResponse();

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody(xmlResponse)));

        HamQslFetchResult result = client.fetch();

        assertNotNull(result);
        SolarIndices solarIndices = result.solarIndices();
        assertNotNull(solarIndices);
        assertEquals(145.5, solarIndices.solarFluxIndex(), 0.01);
        assertEquals(8, solarIndices.aIndex());
        assertEquals(3, solarIndices.kIndex());
        assertEquals(115, solarIndices.sunspotNumber());
        assertEquals(HAMQSL_SOURCE, solarIndices.source());

        wireMockServer.verify(getRequestedFor(urlEqualTo("/")));
    }

    @Test
    void testFetch_NullValues_DefaultsToZero() {
        String xmlResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <solar>
                <solardata>
                    <calculatedconditions>
                        <band name="30m-20m" time="day">Good</band>
                    </calculatedconditions>
                </solardata>
            </solar>
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody(xmlResponse)));

        HamQslFetchResult result = client.fetch();

        assertNotNull(result);
        SolarIndices solarIndices = result.solarIndices();
        assertEquals(0.0, solarIndices.solarFluxIndex(), 0.01);
        assertEquals(0, solarIndices.aIndex());
        assertEquals(0, solarIndices.kIndex());
        assertEquals(0, solarIndices.sunspotNumber());
    }

    // ========== Band Conditions Tests ==========

    @Test
    void testFetch_ValidResponse_ExtractsBandConditions() {
        String xmlResponse = createValidXmlResponse();

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody(xmlResponse)));

        HamQslFetchResult result = client.fetch();

        assertNotNull(result);
        List<BandCondition> bandConditions = result.bandConditions();
        assertNotNull(bandConditions);
        assertEquals(8, bandConditions.size()); // 8 bands: 80m through 10m

        // Check 20m is Good (from "30m-20m" range)
        BandCondition band20m = bandConditions.stream()
                .filter(bc -> bc.band() == FrequencyBand.BAND_20M)
                .findFirst()
                .orElseThrow();
        assertEquals(BandConditionRating.GOOD, band20m.rating());

        // Check 40m is Poor (from "80m-40m" range)
        BandCondition band40m = bandConditions.stream()
                .filter(bc -> bc.band() == FrequencyBand.BAND_40M)
                .findFirst()
                .orElseThrow();
        assertEquals(BandConditionRating.POOR, band40m.rating());

        // Check 15m is Fair (from "17m-15m" range)
        BandCondition band15m = bandConditions.stream()
                .filter(bc -> bc.band() == FrequencyBand.BAND_15M)
                .findFirst()
                .orElseThrow();
        assertEquals(BandConditionRating.FAIR, band15m.rating());
    }

    @Test
    void testFetch_MissingBandConditions_ReturnsEmptyList() {
        String xmlResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <solar>
                <solardata>
                    <solarflux>145.5</solarflux>
                </solardata>
            </solar>
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody(xmlResponse)));

        HamQslFetchResult result = client.fetch();

        assertNotNull(result);
        assertTrue(result.bandConditions().isEmpty());
    }

    @Test
    void testFetch_PartialBandData_ExtractsAvailableBands() {
        String xmlResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <solar>
                <solardata>
                    <calculatedconditions>
                        <band name="30m-20m" time="day">Good</band>
                    </calculatedconditions>
                </solardata>
            </solar>
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody(xmlResponse)));

        HamQslFetchResult result = client.fetch();

        assertNotNull(result);
        assertEquals(2, result.bandConditions().size()); // "30m-20m" expands to 2 bands
    }

    @Test
    void testFetch_UnknownRating_MapsToUnknown() {
        String xmlResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <solar>
                <solardata>
                    <calculatedconditions>
                        <band name="30m-20m" time="day">Excellent</band>
                    </calculatedconditions>
                </solardata>
            </solar>
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody(xmlResponse)));

        HamQslFetchResult result = client.fetch();

        assertNotNull(result);
        assertEquals(2, result.bandConditions().size());
        assertTrue(result.bandConditions().stream()
                .allMatch(bc -> bc.rating() == BandConditionRating.UNKNOWN));
    }

    @Test
    void testFetch_DayAndNightConditions_FiltersToDayOnly() {
        String xmlResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <solar>
                <solardata>
                    <calculatedconditions>
                        <band name="30m-20m" time="day">Good</band>
                        <band name="30m-20m" time="night">Poor</band>
                    </calculatedconditions>
                </solardata>
            </solar>
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody(xmlResponse)));

        HamQslFetchResult result = client.fetch();

        assertNotNull(result);
        assertEquals(2, result.bandConditions().size()); // Only day conditions
        assertTrue(result.bandConditions().stream()
                .allMatch(bc -> bc.rating() == BandConditionRating.GOOD));
    }

    @Test
    void testFetch_UnknownBandRange_Skipped() {
        String xmlResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <solar>
                <solardata>
                    <calculatedconditions>
                        <band name="160m" time="day">Poor</band>
                    </calculatedconditions>
                </solardata>
            </solar>
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody(xmlResponse)));

        HamQslFetchResult result = client.fetch();

        assertNotNull(result);
        assertTrue(result.bandConditions().isEmpty());
    }

    // ========== Error Handling Tests ==========

    @Test
    void testFetch_EmptyResponse_ThrowsException() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody("")));

        assertThrows(InvalidApiResponseException.class, () -> client.fetch());
    }

    @Test
    void testFetch_ServerError_ThrowsException() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        assertThrows(ExternalApiException.class, () -> client.fetch());
    }

    @Test
    void testFetch_MalformedXml_ThrowsException() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody("<invalid>xml")));

        assertThrows(InvalidApiResponseException.class, () -> client.fetch());
    }

    @Test
    void testFetch_MissingSolarData_ThrowsException() {
        String xmlResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><solar></solar>";

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody(xmlResponse)));

        assertThrows(InvalidApiResponseException.class, () -> client.fetch());
    }

    @Test
    void testFetch_DoctypeDeclaration_HandlesSafely() {
        String xmlResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE solar PUBLIC "-//hamqsl//DTD Solar//EN" "http://www.hamqsl.com/solar.dtd">
            <solar>
                <solardata>
                    <solarflux>145.5</solarflux>
                    <aindex>8</aindex>
                    <kindex>3</kindex>
                    <sunspots>115</sunspots>
                </solardata>
            </solar>
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody(xmlResponse)));

        HamQslFetchResult result = client.fetch();

        assertNotNull(result);
        assertEquals(145.5, result.solarIndices().solarFluxIndex(), 0.01);
    }

    // ========== Metadata Tests ==========

    @Test
    void testGetSourceName_ReturnsHamQSL() {
        assertEquals(HAMQSL_SOURCE, client.getSourceName());
    }

    @Test
    void testFreshness_TrackedAfterSuccessfulFetch() {
        String xmlResponse = createValidXmlResponse();

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody(xmlResponse)));

        client.fetch();

        assertNotNull(client.getLastSuccessfulRefresh());
        assertNotNull(client.getDataAge());
    }

    @Test
    void testFreshness_NotUpdatedAfterFailedFetch() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        assertThrows(ExternalApiException.class, () -> client.fetch());

        assertNull(client.getLastSuccessfulRefresh());
    }

    @Test
    void testIsStale_TrueWhenNeverRefreshed() {
        assertTrue(client.isStale());
    }

    @Test
    void testIsStale_FalseAfterSuccessfulFetch() {
        String xmlResponse = createValidXmlResponse();

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody(xmlResponse)));

        client.fetch();

        assertFalse(client.isStale());
    }

    private String createValidXmlResponse() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <solar>
                <solardata>
                    <solarflux>145.5</solarflux>
                    <aindex>8</aindex>
                    <kindex>3</kindex>
                    <sunspots>115</sunspots>
                    <calculatedconditions>
                        <band name="80m-40m" time="day">Poor</band>
                        <band name="30m-20m" time="day">Good</band>
                        <band name="17m-15m" time="day">Fair</band>
                        <band name="12m-10m" time="day">Poor</band>
                    </calculatedconditions>
                </solardata>
            </solar>
            """;
    }

    /**
     * Stub subclass that allows URL override for testing.
     */
    static class StubHamQslClient extends HamQslClient {
        StubHamQslClient(
                WebClient.Builder webClientBuilder,
                CircuitBreakerRegistry circuitBreakerRegistry,
                RetryRegistry retryRegistry,
                String testUrl) {
            super(webClientBuilder, circuitBreakerRegistry, retryRegistry, testUrl);
        }
    }
}
