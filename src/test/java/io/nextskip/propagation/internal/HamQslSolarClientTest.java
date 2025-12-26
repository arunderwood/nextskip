package io.nextskip.propagation.internal;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.nextskip.propagation.model.SolarIndices;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.web.reactive.function.client.WebClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for HamQslSolarClient using WireMock.
 */
class HamQslSolarClientTest {

    private static final String HAMQSL_SOURCE = "HamQSL";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_XML = "text/xml";

    private WireMockServer wireMockServer;
    private HamQslSolarClient client;
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
        cacheManager = new ConcurrentMapCacheManager("hamqslSolar");

        client = new StubHamQslSolarClient(webClientBuilder, cacheManager,
                circuitBreakerRegistry, retryRegistry, baseUrl);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void shouldFetch_SolarIndicesSuccessfully() {
        String xmlResponse = createValidXmlResponse();

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody(xmlResponse)));

        SolarIndices result = client.fetch();

        assertNotNull(result);
        assertEquals(145.5, result.solarFluxIndex(), 0.01);
        assertEquals(8, result.aIndex());
        assertEquals(3, result.kIndex());
        assertEquals(115, result.sunspotNumber());
        assertEquals(HAMQSL_SOURCE, result.source());

        wireMockServer.verify(getRequestedFor(urlEqualTo("/")));
    }

    @Test
    void shouldHandle_EmptyResponse() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody("")));

        // Empty response triggers fallback - client returns null for default
        SolarIndices result = client.fetch();
        assertNull(result);
    }

    @Test
    void shouldHandle_ServerError() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // Server error triggers fallback - client returns null for default
        SolarIndices result = client.fetch();
        assertNull(result);
    }

    @Test
    void shouldHandle_MalformedXml() {
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody("<invalid>xml")));

        SolarIndices result = client.fetch();
        assertNull(result);
    }

    @Test
    void shouldHandle_MissingSolarData() {
        String xmlResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><solar></solar>";

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody(xmlResponse)));

        SolarIndices result = client.fetch();
        assertNull(result);
    }

    @Test
    void shouldHandle_NullValues_WithDefaults() {
        // Response with missing numeric values - should default to 0
        String xmlResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <solar>
                <solardata>
                </solardata>
            </solar>
            """;

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody(xmlResponse)));

        SolarIndices result = client.fetch();

        assertNotNull(result);
        assertEquals(0.0, result.solarFluxIndex(), 0.01);
        assertEquals(0, result.aIndex());
        assertEquals(0, result.kIndex());
        assertEquals(0, result.sunspotNumber());
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

    @Test
    void shouldReturn_IsStale_WhenNeverRefreshed() {
        assertTrue(client.isStale());
    }

    @Test
    void shouldReturn_NotStale_AfterSuccessfulFetch() {
        String xmlResponse = createValidXmlResponse();

        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_XML)
                        .withBody(xmlResponse)));

        client.fetch();

        assertFalse(client.isStale());
    }

    @Test
    void shouldHandle_DoctypeDeclaration() {
        // HamQSL sometimes returns malformed DOCTYPE - should still parse
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

        SolarIndices result = client.fetch();

        assertNotNull(result);
        assertEquals(145.5, result.solarFluxIndex(), 0.01);
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
                </solardata>
            </solar>
            """;
    }

    /**
     * Stub subclass that allows URL override for testing.
     */
    static class StubHamQslSolarClient extends HamQslSolarClient {
        StubHamQslSolarClient(
                WebClient.Builder webClientBuilder,
                CacheManager cacheManager,
                CircuitBreakerRegistry circuitBreakerRegistry,
                RetryRegistry retryRegistry,
                String testUrl) {
            super(webClientBuilder, cacheManager, circuitBreakerRegistry, retryRegistry, testUrl);
        }
    }
}
