package io.nextskip.propagation.internal;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.nextskip.common.client.AbstractExternalDataClient;
import io.nextskip.common.client.InvalidApiResponseException;
import io.nextskip.common.model.FrequencyBand;
import io.nextskip.propagation.internal.dto.HamQslDto.BandConditionEntry;
import io.nextskip.propagation.internal.dto.HamQslDto.HamQslData;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.BandConditionRating;
import io.nextskip.propagation.model.SolarIndices;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import javax.xml.stream.XMLInputFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Unified client for HamQSL.com solar and band condition data.
 *
 * <p>Fetches all data from the HamQSL XML feed in a single HTTP request:
 * <ul>
 *   <li>Solar indices: SFI, K-index, A-index, sunspots</li>
 *   <li>Band conditions: 80m-40m, 30m-20m, 17m-15m, 12m-10m ratings</li>
 * </ul>
 *
 * <p>This unified client eliminates duplicate HTTP fetches that occurred when
 * separate clients ({@code HamQslSolarClient} and {@code HamQslBandClient})
 * each fetched the same XML endpoint independently.
 *
 * <p>Extends {@link AbstractExternalDataClient} to inherit:
 * <ul>
 *   <li>Circuit breaker to prevent cascading failures</li>
 *   <li>Retry logic for transient failures</li>
 *   <li>Freshness tracking for UI display</li>
 * </ul>
 *
 * <p>Security: Disables DTD processing to prevent XXE attacks.
 */
@Component
@SuppressWarnings("PMD.AvoidCatchingGenericException") // Intentional: wrap parsing exceptions
public class HamQslClient extends AbstractExternalDataClient<HamQslFetchResult> {

    private static final String CLIENT_NAME = "hamqsl";
    private static final String SOURCE_NAME = "HamQSL";

    /**
     * Refresh interval for data fetching.
     *
     * <p>HamQSL data updates every 3 hours. A 2-hour interval balances freshness
     * with bandwidth efficiency.
     *
     * @see <a href="https://www.hamqsl.com/FAQ.html">HamQSL FAQ - Update Frequencies</a>
     */
    private static final Duration REFRESH_INTERVAL = Duration.ofHours(2);
    private static final String HAMQSL_URL = "https://www.hamqsl.com/solarxml.php";

    // Band condition time period (day vs night)
    private static final String DAY_TIME_PERIOD = "day";

    // Pre-initialized XmlMapper for secure XML parsing
    private static final XmlMapper SHARED_XML_MAPPER = createXmlMapper();

    private final XmlMapper xmlMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public HamQslClient(
            WebClient.Builder webClientBuilder,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry) {
        this(webClientBuilder, circuitBreakerRegistry, retryRegistry, HAMQSL_URL);
    }

    protected HamQslClient(
            WebClient.Builder webClientBuilder,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            String baseUrl) {
        super(webClientBuilder, circuitBreakerRegistry, retryRegistry, baseUrl);
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
    public Duration getRefreshInterval() {
        return REFRESH_INTERVAL;
    }

    @Override
    protected HamQslFetchResult doFetch() {
        getLog().debug("Fetching solar and band data from HamQSL");

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

            // Extract solar indices
            SolarIndices solarIndices = extractSolarIndices(data);

            // Extract band conditions
            List<BandCondition> bandConditions = extractBandConditions(data);

            getLog().info("Successfully fetched from HamQSL: K={}, A={}, {} band conditions",
                    solarIndices.kIndex(), solarIndices.aIndex(), bandConditions.size());

            return new HamQslFetchResult(solarIndices, bandConditions);

        } catch (InvalidApiResponseException e) {
            throw e;
        } catch (Exception e) {
            getLog().error("Error parsing HamQSL data", e);
            throw new InvalidApiResponseException(SOURCE_NAME,
                    "Failed to parse HamQSL data: " + e.getMessage(), e);
        }
    }

    // ========== Data Extraction ==========

    private SolarIndices extractSolarIndices(HamQslData data) {
        Double solarFlux = data.getSolarFlux() != null ? data.getSolarFlux() : 0.0;
        Integer aIndex = data.getAIndex() != null ? data.getAIndex() : 0;
        Integer kIndex = data.getKIndex() != null ? data.getKIndex() : 0;
        Integer sunspots = data.getSunspots() != null ? data.getSunspots() : 0;

        return new SolarIndices(
                solarFlux,
                aIndex,
                kIndex,
                sunspots,
                Instant.now(),
                SOURCE_NAME
        );
    }

    private List<BandCondition> extractBandConditions(HamQslData data) {
        List<BandCondition> conditions = new ArrayList<>();

        var solarData = data.getSolardata();
        if (solarData.getCalculatedConditions() != null
                && solarData.getCalculatedConditions().getBands() != null) {

            for (BandConditionEntry entry : solarData.getCalculatedConditions().getBands()) {
                // Use "day" conditions
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

        return conditions;
    }

    // ========== Band Rating Parsing ==========

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
