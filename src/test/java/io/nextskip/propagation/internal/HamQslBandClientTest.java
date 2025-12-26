package io.nextskip.propagation.internal;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.nextskip.common.model.FrequencyBand;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.BandConditionRating;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for HamQslBandClient using WireMock.
 */
class HamQslBandClientTest {

    private static final String HAMQSL_SOURCE = "HamQSL";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_XML = "text/xml";

    private WireMockServer wireMockServer;
    private HamQslBandClient client;
    private CacheManager cacheManager;
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
        cacheManager = new ConcurrentMapCacheManager("hamqslBand");

        client = new StubHamQslBandClient(webClientBuilder, cacheManager,
                circuitBreakerRegistry, retryRegistry, baseUrl);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void shouldFetch_BandConditionsSuccessfully() {
        String xmlResponse = createValidXmlResponse();

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody(xmlResponse)));

        List<BandCondition> result = client.fetch();

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

        wireMockServer.verify(getRequestedFor(urlEqualTo("/")));
    }

    @Test
    void shouldHandle_EmptyResponse() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody("")));

        // Empty response triggers fallback - returns empty list
        List<BandCondition> result = client.fetch();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandle_ServerError() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // Server error triggers fallback - returns empty list
        List<BandCondition> result = client.fetch();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandle_MalformedXml() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody("<invalid>xml")));

        List<BandCondition> result = client.fetch();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandle_MissingSolarData() {
        String xmlResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><solar></solar>";

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody(xmlResponse)));

        List<BandCondition> result = client.fetch();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandle_MissingBandConditions() {
        // Response with solardata but no calculatedconditions
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

        List<BandCondition> result = client.fetch();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandle_PartialBandData() {
        // Only one band range in response
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

        List<BandCondition> result = client.fetch();

        assertNotNull(result);
        assertEquals(2, result.size()); // "30m-20m" expands to 2 bands
    }

    @Test
    void shouldHandle_UnknownRating() {
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

        List<BandCondition> result = client.fetch();

        assertNotNull(result);
        assertEquals(2, result.size());
        // Unknown rating should map to UNKNOWN
        assertTrue(result.stream().allMatch(bc -> bc.rating() == BandConditionRating.UNKNOWN));
    }

    @Test
    void shouldHandle_BlankRating() {
        String xmlResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <solar>
                <solardata>
                    <calculatedconditions>
                        <band name="30m-20m" time="day">  </band>
                    </calculatedconditions>
                </solardata>
            </solar>
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody(xmlResponse)));

        List<BandCondition> result = client.fetch();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(bc -> bc.rating() == BandConditionRating.UNKNOWN));
    }

    @Test
    void shouldFilter_ToOnlyDayConditions() {
        // Response with both day and night conditions
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

        List<BandCondition> result = client.fetch();

        assertNotNull(result);
        assertEquals(2, result.size()); // Only day conditions
        assertTrue(result.stream().allMatch(bc -> bc.rating() == BandConditionRating.GOOD));
    }

    @Test
    void shouldHandle_UnknownBandRange() {
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

        List<BandCondition> result = client.fetch();

        assertNotNull(result);
        assertTrue(result.isEmpty()); // Unknown band range is skipped
    }

    @Test
    void shouldReturn_SourceName() {
        assertEquals(HAMQSL_SOURCE, client.getSourceName());
    }

    @Test
    void shouldTrack_FreshnessAfterSuccessfulFetch() {
        String xmlResponse = createValidXmlResponse();

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody(xmlResponse)));

        client.fetch();

        assertNotNull(client.getLastSuccessfulRefresh());
        assertFalse(client.isServingStaleData());
        assertNotNull(client.getDataAge());
    }

    @Test
    void shouldTrack_StaleDataAfterFailedFetch() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        client.fetch();

        assertTrue(client.isServingStaleData());
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
    static class StubHamQslBandClient extends HamQslBandClient {
        StubHamQslBandClient(
                WebClient.Builder webClientBuilder,
                CacheManager cacheManager,
                CircuitBreakerRegistry circuitBreakerRegistry,
                RetryRegistry retryRegistry,
                String testUrl) {
            super(webClientBuilder, cacheManager, circuitBreakerRegistry, retryRegistry, testUrl);
        }
    }
}
