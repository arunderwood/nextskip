package io.nextskip.propagation.internal;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.nextskip.common.client.AbstractExternalDataClient;
import io.nextskip.common.client.InvalidApiResponseException;
import io.nextskip.propagation.internal.dto.HamQslDto.HamQslData;
import io.nextskip.propagation.model.SolarIndices;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import javax.xml.stream.XMLInputFactory;
import java.time.Duration;
import java.time.Instant;

/**
 * Client for HamQSL.com solar index data.
 *
 * <p>Fetches solar indices from the HamQSL XML feed:
 * <ul>
 *   <li>Solar Flux Index (SFI)</li>
 *   <li>K-index (geomagnetic activity)</li>
 *   <li>A-index (daily geomagnetic activity)</li>
 *   <li>Sunspot number</li>
 * </ul>
 *
 * <p>Extends {@link AbstractExternalDataClient} to inherit:
 * <ul>
 *   <li>Circuit breaker to prevent cascading failures</li>
 *   <li>Retry logic for transient failures</li>
 *   <li>Cache fallback on failures</li>
 *   <li>Freshness tracking for UI display</li>
 * </ul>
 *
 * <p>Security: Disables DTD processing to prevent XXE attacks.
 */
@Component
@SuppressWarnings("PMD.AvoidCatchingGenericException") // Intentional: wrap parsing exceptions
public class HamQslSolarClient extends AbstractExternalDataClient<SolarIndices> {

    private static final String CLIENT_NAME = "hamqsl-solar";
    private static final String SOURCE_NAME = "HamQSL";
    private static final String CACHE_NAME = "hamqslSolar";
    private static final String CACHE_KEY = "indices";
    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(5);
    private static final String HAMQSL_URL = "https://www.hamqsl.com/solarxml.php";

    // Pre-initialized XmlMapper for secure XML parsing
    private static final XmlMapper SHARED_XML_MAPPER = createXmlMapper();

    private final XmlMapper xmlMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public HamQslSolarClient(
            WebClient.Builder webClientBuilder,
            CacheManager cacheManager,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry) {
        this(webClientBuilder, cacheManager, circuitBreakerRegistry, retryRegistry, HAMQSL_URL);
    }

    protected HamQslSolarClient(
            WebClient.Builder webClientBuilder,
            CacheManager cacheManager,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            String baseUrl) {
        super(webClientBuilder, cacheManager, circuitBreakerRegistry, retryRegistry, baseUrl);
        this.xmlMapper = SHARED_XML_MAPPER;
    }

    // ========== AbstractExternalDataClient implementation ==========

    @Override
    protected String getClientName() {
        return CLIENT_NAME;
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    @Override
    protected String getCacheName() {
        return CACHE_NAME;
    }

    @Override
    protected String getCacheKey() {
        return CACHE_KEY;
    }

    @Override
    public Duration getRefreshInterval() {
        return REFRESH_INTERVAL;
    }

    @Override
    protected SolarIndices doFetch() {
        getLog().debug("Fetching solar data from HamQSL");

        String xml = getWebClient().get()
                .retrieve()
                .bodyToMono(String.class)
                .timeout(getRequestTimeout())
                .block();

        if (xml == null || xml.isBlank()) {
            getLog().warn("No data received from HamQSL");
            throw new InvalidApiResponseException(SOURCE_NAME, "Empty response from HamQSL API");
        }

        try {
            HamQslData data = xmlMapper.readValue(xml, HamQslData.class);

            if (data == null || data.getSolardata() == null) {
                throw new InvalidApiResponseException(SOURCE_NAME, "Missing solardata element in XML response");
            }

            // Validate the data
            data.getSolardata().validate();

            // Extract values with defaults
            Double solarFlux = data.getSolarFlux() != null ? data.getSolarFlux() : 0.0;
            Integer aIndex = data.getAIndex() != null ? data.getAIndex() : 0;
            Integer kIndex = data.getKIndex() != null ? data.getKIndex() : 0;
            Integer sunspots = data.getSunspots() != null ? data.getSunspots() : 0;

            SolarIndices indices = new SolarIndices(
                    solarFlux,
                    aIndex,
                    kIndex,
                    sunspots,
                    Instant.now(),
                    SOURCE_NAME
            );

            getLog().info("Successfully fetched solar indices from HamQSL: K={}, A={}", kIndex, aIndex);
            return indices;

        } catch (InvalidApiResponseException e) {
            throw e;
        } catch (Exception e) {
            getLog().error("Error parsing HamQSL solar data", e);
            throw new InvalidApiResponseException(SOURCE_NAME,
                    "Failed to parse HamQSL solar data: " + e.getMessage(), e);
        }
    }

    @Override
    protected SolarIndices getDefaultValue() {
        return null; // No sensible default for solar indices
    }

    // ========== XML Security Configuration ==========

    /**
     * Creates an XmlMapper configured for robust and secure XML parsing.
     *
     * <p>Disables DTD processing to prevent XXE attacks and handle malformed DOCTYPE declarations.
     * Configured to ignore unknown XML elements for forward compatibility.
     */
    private static XmlMapper createXmlMapper() {
        // Create XMLInputFactory with security settings
        XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();

        // Disable DTD processing entirely to:
        // 1. Prevent XXE (XML External Entity) attacks
        // 2. Handle malformed DOCTYPE declarations gracefully
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

        // Create XmlMapper with the secure factory
        XmlMapper mapper = XmlMapper.builder()
                .defaultUseWrapper(false)
                .build();

        mapper.getFactory().setXMLInputFactory(xmlInputFactory);

        // Configure deserialization features for robustness
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(FromXmlParser.Feature.EMPTY_ELEMENT_AS_NULL, false);

        return mapper;
    }
}
