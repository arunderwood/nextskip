package io.nextskip.propagation.internal;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.nextskip.common.client.RefreshableDataSource;
import io.nextskip.common.model.FrequencyBand;
import io.nextskip.propagation.internal.dto.HamQslDto.BandConditionEntry;
import io.nextskip.propagation.internal.dto.HamQslDto.HamQslData;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.BandConditionRating;
import io.nextskip.propagation.model.SolarIndices;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.xml.stream.XMLInputFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for HamQSL.com solar and band condition data.
 *
 * Fetches XML data containing:
 * - Solar flux index
 * - K-index and A-index
 * - Band conditions for all HF bands
 *
 * Features:
 * - Circuit breaker pattern
 * - Retry logic
 * - 30-minute cache TTL (per HamQSL recommendations)
 * - Fallback to cached data
 */
@Component
@SuppressWarnings("PMD.AvoidCatchingGenericException") // Intentional: wrap unknown exceptions in ExternalApiException
public class HamQslClient implements RefreshableDataSource {

    private static final Logger LOG = LoggerFactory.getLogger(HamQslClient.class);

    private static final String SOURCE_NAME = "HamQSL";
    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(5);
    private static final String CACHE_KEY = "hamqsl";
    private static final String HAMQSL_URL = "https://www.hamqsl.com/solarxml.php";

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    // Band condition time period (day vs night)
    private static final String DAY_TIME_PERIOD = "day";

    // Pre-initialized XmlMapper to avoid constructor exceptions (SpotBugs CT_CONSTRUCTOR_THROW)
    private static final XmlMapper SHARED_XML_MAPPER = createXmlMapper();

    private final WebClient webClient;
    private final CacheManager cacheManager;
    private final XmlMapper xmlMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public HamQslClient(WebClient.Builder webClientBuilder, CacheManager cacheManager) {
        this(webClientBuilder, cacheManager, HAMQSL_URL);
    }

    protected HamQslClient(WebClient.Builder webClientBuilder, CacheManager cacheManager, String baseUrl) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(1024 * 1024)) // 1MB limit
                .build();
        this.cacheManager = cacheManager;

        // Use pre-initialized XmlMapper to avoid constructor exceptions
        this.xmlMapper = SHARED_XML_MAPPER;
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    @Override
    public void refresh() {
        fetchSolarIndices();
        fetchBandConditions();
    }

    @Override
    public Duration getRefreshInterval() {
        return REFRESH_INTERVAL;
    }

    /**
     * Creates an XmlMapper configured for robust and secure XML parsing.
     *
     * Disables DTD processing to prevent XXE attacks and handle malformed DOCTYPE declarations.
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

    /**
     * Fetch solar indices from HamQSL.
     *
     * @return SolarIndices with K/A index data
     * @throws ExternalApiException if the API call fails
     * @throws InvalidApiResponseException if the response is invalid
     */
    @CircuitBreaker(name = CACHE_KEY, fallbackMethod = "getCachedSolarData")
    @Retry(name = CACHE_KEY)
    @Cacheable(value = "solarIndices", key = "'" + CACHE_KEY + "'", unless = "#result == null")
    public SolarIndices fetchSolarIndices() {
        LOG.debug("Fetching solar data from HamQSL");

        try {
            String xml = webClient.get()
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            if (xml == null || xml.isBlank()) {
                LOG.warn("No data received from HamQSL");
                throw new InvalidApiResponseException(SOURCE_NAME, "Empty response from HamQSL API");
            }

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

            LOG.info("Successfully fetched solar indices from HamQSL: K={}, A={}", kIndex, aIndex);
            return indices;

        } catch (WebClientResponseException e) {
            LOG.error("HTTP error from HamQSL API: {} {}", e.getStatusCode(), e.getStatusText());
            throw new ExternalApiException(SOURCE_NAME,
                    "HTTP " + e.getStatusCode() + " from HamQSL API: " + e.getStatusText(), e);

        } catch (WebClientRequestException e) {
            LOG.error("Network error connecting to HamQSL API", e);
            throw new ExternalApiException(SOURCE_NAME,
                    "Network error connecting to HamQSL API: " + e.getMessage(), e);

        } catch (InvalidApiResponseException e) {
            LOG.error("Invalid response from HamQSL API: {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            LOG.error("Error fetching solar data from HamQSL", e);
            throw new ExternalApiException(SOURCE_NAME,
                    "Failed to parse HamQSL solar data: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch band conditions from HamQSL.
     *
     * @return List of band conditions
     * @throws ExternalApiException if the API call fails
     * @throws InvalidApiResponseException if the response is invalid
     */
    @CircuitBreaker(name = CACHE_KEY, fallbackMethod = "getCachedBandConditions")
    @Retry(name = CACHE_KEY)
    @Cacheable(value = "bandConditions", key = "'" + CACHE_KEY + "'", unless = "#result == null || #result.isEmpty()")
    public List<BandCondition> fetchBandConditions() {
        LOG.debug("Fetching band conditions from HamQSL");

        try {
            String xml = webClient.get()
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            if (xml == null || xml.isBlank()) {
                LOG.warn("No data received from HamQSL");
                throw new InvalidApiResponseException(SOURCE_NAME, "Empty response from HamQSL API");
            }

            HamQslData data = xmlMapper.readValue(xml, HamQslData.class);

            if (data == null || data.getSolardata() == null) {
                throw new InvalidApiResponseException(SOURCE_NAME, "Missing solardata element in XML response");
            }

            List<BandCondition> conditions = new ArrayList<>();

            // Parse band conditions from the XML structure
            var solarData = data.getSolardata();
            if (solarData.getCalculatedConditions() != null
                    && solarData.getCalculatedConditions().getBands() != null) {

                for (BandConditionEntry entry : solarData.getCalculatedConditions().getBands()) {
                    // Use "day" conditions for now (could be enhanced to handle time-based selection)
                    if (DAY_TIME_PERIOD.equals(entry.getTime())) {
                        BandConditionRating rating = parseBandRating(entry.getValue());

                        // Map band ranges to individual bands
                        switch (entry.getName()) {
                            case "80m-40m":
                                conditions.add(new BandCondition(FrequencyBand.BAND_80M, rating));
                                conditions.add(new BandCondition(FrequencyBand.BAND_40M, rating));
                                break;
                            case "30m-20m":
                                conditions.add(new BandCondition(FrequencyBand.BAND_30M, rating));
                                conditions.add(new BandCondition(FrequencyBand.BAND_20M, rating));
                                break;
                            case "17m-15m":
                                conditions.add(new BandCondition(FrequencyBand.BAND_17M, rating));
                                conditions.add(new BandCondition(FrequencyBand.BAND_15M, rating));
                                break;
                            case "12m-10m":
                                conditions.add(new BandCondition(FrequencyBand.BAND_12M, rating));
                                conditions.add(new BandCondition(FrequencyBand.BAND_10M, rating));
                                break;
                            default:
                                // Unknown band range, skip
                                break;
                        }
                    }
                }
            }

            LOG.info("Successfully fetched {} band conditions from HamQSL", conditions.size());
            return conditions;

        } catch (WebClientResponseException e) {
            LOG.error("HTTP error from HamQSL API: {} {}", e.getStatusCode(), e.getStatusText());
            throw new ExternalApiException(SOURCE_NAME,
                    "HTTP " + e.getStatusCode() + " from HamQSL API: " + e.getStatusText(), e);

        } catch (WebClientRequestException e) {
            LOG.error("Network error connecting to HamQSL API", e);
            throw new ExternalApiException(SOURCE_NAME,
                    "Network error connecting to HamQSL API: " + e.getMessage(), e);

        } catch (InvalidApiResponseException e) {
            LOG.error("Invalid response from HamQSL API: {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            LOG.error("Error fetching band conditions from HamQSL", e);
            throw new ExternalApiException(SOURCE_NAME,
                    "Failed to parse HamQSL band conditions: " + e.getMessage(), e);
        }
    }

    private BandConditionRating parseBandRating(String value) {
        if (value == null || value.isBlank()) {
            return BandConditionRating.UNKNOWN;
        }
        return switch (value.toLowerCase(Locale.ROOT).trim()) {
            case "good" -> BandConditionRating.GOOD;
            case "fair" -> BandConditionRating.FAIR;
            case "poor" -> BandConditionRating.POOR;
            default -> BandConditionRating.UNKNOWN;
        };
    }

    @SuppressWarnings("unused")
    private SolarIndices getCachedSolarData(Exception e) {
        LOG.warn("Using cached solar data due to: {}", e.getMessage());
        var cache = cacheManager.getCache("solarIndices");
        if (cache != null) {
            return cache.get(CACHE_KEY, SolarIndices.class);
        }
        return null;
    }

    @SuppressWarnings("unused")
    private List<BandCondition> getCachedBandConditions(Exception e) {
        LOG.warn("Using cached band conditions due to: {}", e.getMessage());
        var cache = cacheManager.getCache("bandConditions");
        if (cache != null) {
            @SuppressWarnings("unchecked")
            List<BandCondition> cached = (List<BandCondition>) cache.get(CACHE_KEY, List.class);
            if (cached != null) {
                return cached;
            }
        }
        return List.of();
    }
}
