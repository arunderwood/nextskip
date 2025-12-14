package io.nextskip.propagation.internal;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.nextskip.common.model.FrequencyBand;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.BandConditionRating;
import io.nextskip.propagation.model.SolarIndices;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for HamQslClient using WireMock.
 */
class HamQslClientTest {

    private WireMockServer wireMockServer;
    private HamQslClient client;
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();

        String baseUrl = "http://localhost:" + wireMockServer.port();
        WebClient.Builder webClientBuilder = WebClient.builder();
        cacheManager = new ConcurrentMapCacheManager("solarIndices", "bandConditions");

        client = new TestHamQslClient(webClientBuilder, cacheManager, baseUrl);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void testFetchSolarIndices_Success() {
        String xmlResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<solar>" +
                "<solardata>" +
                "<solarflux>145.5</solarflux>" +
                "<aindex>8</aindex>" +
                "<kindex>3</kindex>" +
                "<sunspots>115</sunspots>" +
                "</solardata>" +
                "</solar>";

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(xmlResponse)));

        SolarIndices result = client.fetchSolarIndices();

        assertNotNull(result);
        assertEquals(145.5, result.solarFluxIndex(), 0.01);
        assertEquals(8, result.aIndex());
        assertEquals(3, result.kIndex());
        assertEquals(115, result.sunspotNumber());
        assertEquals("HamQSL", result.source());

        wireMockServer.verify(getRequestedFor(urlEqualTo("/")));
    }

    @Test
    void testFetchBandConditions_Success() {
        String xmlResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<solar>" +
                "<solardata>" +
                "<solarflux>145.5</solarflux>" +
                "<aindex>8</aindex>" +
                "<kindex>3</kindex>" +
                "<sunspots>115</sunspots>" +
                "<calculatedconditions>" +
                "<band name=\"80m-40m\" time=\"day\">Poor</band>" +
                "<band name=\"30m-20m\" time=\"day\">Good</band>" +
                "<band name=\"17m-15m\" time=\"day\">Fair</band>" +
                "<band name=\"12m-10m\" time=\"day\">Poor</band>" +
                "</calculatedconditions>" +
                "</solardata>" +
                "</solar>";

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(xmlResponse)));

        List<BandCondition> result = client.fetchBandConditions();

        assertNotNull(result);
        assertEquals(8, result.size()); // 8 bands: 80m through 10m

        // Check 20m is Good (from "30m-20m" range)
        BandCondition band20m = result.stream()
                .filter(bc -> bc.band() == FrequencyBand.BAND_20M)
                .findFirst()
                .orElseThrow();
        assertEquals(BandConditionRating.GOOD, band20m.rating());

        // Check 40m is Poor (from "80m-40m" range)
        BandCondition band40m = result.stream()
                .filter(bc -> bc.band() == FrequencyBand.BAND_40M)
                .findFirst()
                .orElseThrow();
        assertEquals(BandConditionRating.POOR, band40m.rating());

        // Check 15m is Fair (from "17m-15m" range)
        BandCondition band15m = result.stream()
                .filter(bc -> bc.band() == FrequencyBand.BAND_15M)
                .findFirst()
                .orElseThrow();
        assertEquals(BandConditionRating.FAIR, band15m.rating());
    }

    @Test
    void testFetchBandConditions_MissingBands() {
        String xmlResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<solar>" +
                "<solardata>" +
                "<solarflux>145.5</solarflux>" +
                "<aindex>8</aindex>" +
                "<kindex>3</kindex>" +
                "<sunspots>115</sunspots>" +
                "<calculatedconditions>" +
                "<band name=\"30m-20m\" time=\"day\">Good</band>" +
                "</calculatedconditions>" +
                "</solardata>" +
                "</solar>";

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(xmlResponse)));

        List<BandCondition> result = client.fetchBandConditions();

        assertNotNull(result);
        assertEquals(2, result.size()); // "30m-20m" expands to 2 bands
    }

    @Test
    void testFetchBandConditions_InvalidRating() {
        String xmlResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<solar>" +
                "<solardata>" +
                "<solarflux>145.5</solarflux>" +
                "<aindex>8</aindex>" +
                "<kindex>3</kindex>" +
                "<sunspots>115</sunspots>" +
                "<calculatedconditions>" +
                "<band name=\"30m-20m\" time=\"day\">Excellent</band>" +
                "</calculatedconditions>" +
                "</solardata>" +
                "</solar>";

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(xmlResponse)));

        List<BandCondition> result = client.fetchBandConditions();

        assertNotNull(result);
        assertEquals(2, result.size()); // "30m-20m" expands to 2 bands

        // Invalid rating should be parsed as UNKNOWN for both bands
        assertTrue(result.stream().allMatch(bc -> bc.rating() == BandConditionRating.UNKNOWN));
    }

    @Test
    void testFetchSolarIndices_ServerError() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        assertThrows(RuntimeException.class, () -> client.fetchSolarIndices());
    }

    @Test
    void testFetchBandConditions_ServerError() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        assertThrows(RuntimeException.class, () -> client.fetchBandConditions());
    }

    @Test
    void testFetchSolarIndices_EmptyResponse() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("")));

        // Empty response should throw InvalidApiResponseException
        assertThrows(io.nextskip.propagation.internal.InvalidApiResponseException.class,
                () -> client.fetchSolarIndices());
    }

    @Test
    void testFetchBandConditions_EmptyResponse() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("")));

        // Empty response should throw InvalidApiResponseException
        assertThrows(io.nextskip.propagation.internal.InvalidApiResponseException.class,
                () -> client.fetchBandConditions());
    }

    @Test
    void testFetchSolarIndices_MalformedXml() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<solar><invalid></solar>")));

        assertThrows(RuntimeException.class, () -> client.fetchSolarIndices());
    }

    @Test
    void testBandConditionRating_CaseInsensitive() {
        String xmlResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<solar>" +
                "<solardata>" +
                "<solarflux>145.5</solarflux>" +
                "<aindex>8</aindex>" +
                "<kindex>3</kindex>" +
                "<sunspots>115</sunspots>" +
                "<calculatedconditions>" +
                "<band name=\"30m-20m\" time=\"day\">GOOD</band>" +
                "<band name=\"80m-40m\" time=\"day\">fair</band>" +
                "<band name=\"12m-10m\" time=\"day\">Poor</band>" +
                "</calculatedconditions>" +
                "</solardata>" +
                "</solar>";

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(xmlResponse)));

        List<BandCondition> result = client.fetchBandConditions();

        assertEquals(6, result.size()); // 3 ranges expand to 6 bands

        // All ratings should be parsed correctly regardless of case
        assertTrue(result.stream().anyMatch(bc -> bc.rating() == BandConditionRating.GOOD));
        assertTrue(result.stream().anyMatch(bc -> bc.rating() == BandConditionRating.FAIR));
        assertTrue(result.stream().anyMatch(bc -> bc.rating() == BandConditionRating.POOR));
    }

    /**
     * Test subclass that allows URL override for testing.
     */
    static class TestHamQslClient extends HamQslClient {
        TestHamQslClient(WebClient.Builder webClientBuilder, CacheManager cacheManager, String testUrl) {
            super(webClientBuilder, cacheManager, testUrl);
        }
    }
}
