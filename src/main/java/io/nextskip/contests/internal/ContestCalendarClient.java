package io.nextskip.contests.internal;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.nextskip.common.client.ExternalDataClient;
import io.nextskip.contests.internal.dto.ContestICalDto;
import io.nextskip.propagation.internal.ExternalApiException;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Client for WA7BNM Contest Calendar iCal feed.
 *
 * <p>Fetches upcoming amateur radio contests from the WA7BNM weekly iCal feed
 * at https://www.contestcalendar.com/weeklycontcustom.php
 *
 * <p>The iCal feed provides a pre-filtered 8-day window of upcoming contests with:
 * <ul>
 *   <li>Contest names (SUMMARY)</li>
 *   <li>Start/end times (DTSTART/DTEND in UTC)</li>
 *   <li>Details page URLs (URL property)</li>
 * </ul>
 *
 * <p>Features:
 * <ul>
 *   <li>Circuit breaker to prevent cascading failures</li>
 *   <li>Retry logic for transient failures</li>
 *   <li>30-minute cache TTL (contests don't change frequently)</li>
 *   <li>Fallback to cached data on failures</li>
 * </ul>
 */
@org.springframework.stereotype.Component
@SuppressWarnings("PMD.AvoidCatchingGenericException") // Intentional: wrap unknown exceptions in ExternalApiException
public class ContestCalendarClient implements ExternalDataClient<List<ContestICalDto>> {

    private static final Logger LOG = LoggerFactory.getLogger(ContestCalendarClient.class);

    private static final String SOURCE_NAME = "WA7BNM";
    private static final String CACHE_NAME = "contests";
    private static final String CALENDAR_URL = "https://www.contestcalendar.com/weeklycontcustom.php";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final WebClient webClient;
    private final CacheManager cacheManager;

    @org.springframework.beans.factory.annotation.Autowired
    public ContestCalendarClient(WebClient.Builder webClientBuilder, CacheManager cacheManager) {
        this(webClientBuilder, cacheManager, CALENDAR_URL);
    }

    protected ContestCalendarClient(WebClient.Builder webClientBuilder, CacheManager cacheManager, String baseUrl) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(1024 * 1024)) // 1MB limit
                .build();
        this.cacheManager = cacheManager;
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME + " Contest Calendar";
    }

    /**
     * Fetch upcoming contests from WA7BNM iCal feed.
     *
     * @return List of contest DTOs parsed from iCal, or empty list if unavailable
     * @throws ExternalApiException if the API call fails
     */
    @Override
    @CircuitBreaker(name = CACHE_NAME, fallbackMethod = "getCachedContests")
    @Retry(name = CACHE_NAME)
    @Cacheable(value = CACHE_NAME, key = "'upcoming'", unless = "#result == null")
    public List<ContestICalDto> fetch() {
        LOG.debug("Fetching contests from WA7BNM iCal feed");

        try {
            // Fetch iCal data as string
            String icalData = webClient.get()
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            if (icalData == null || icalData.isBlank()) {
                LOG.warn("No data received from WA7BNM contest calendar");
                throw new ExternalApiException(SOURCE_NAME, "Empty response from contest calendar");
            }

            // Parse iCal using ical4j
            List<ContestICalDto> contests = parseICalData(icalData);

            LOG.info("Successfully fetched {} contests from WA7BNM", contests.size());
            return contests;

        } catch (WebClientResponseException e) {
            // HTTP error (4xx, 5xx)
            LOG.error("HTTP error from WA7BNM: {} {}", e.getStatusCode(), e.getStatusText());
            throw new ExternalApiException(SOURCE_NAME,
                    "HTTP " + e.getStatusCode() + " from WA7BNM: " + e.getStatusText(), e);

        } catch (WebClientRequestException e) {
            // Network error
            LOG.error("Network error connecting to WA7BNM", e);
            throw new ExternalApiException(SOURCE_NAME,
                    "Network error connecting to WA7BNM: " + e.getMessage(), e);

        } catch (ExternalApiException e) {
            // Already wrapped - just rethrow
            throw e;

        } catch (Exception e) {
            // Unexpected error (including iCal parsing errors)
            LOG.error("Unexpected error fetching contests from WA7BNM", e);
            throw new ExternalApiException(SOURCE_NAME,
                    "Unexpected error fetching contest data: " + e.getMessage(), e);
        }
    }

    /**
     * Parse iCal data into contest DTOs.
     *
     * @param icalData raw iCal string from WA7BNM
     * @return list of parsed contest DTOs
     */
    private List<ContestICalDto> parseICalData(String icalData) {
        List<ContestICalDto> contests = new ArrayList<>();

        try {
            CalendarBuilder builder = new CalendarBuilder();
            Calendar calendar = builder.build(new StringReader(icalData));

            // Extract all VEVENT components
            List<VEvent> events = calendar.getComponents(net.fortuna.ical4j.model.Component.VEVENT);

            for (VEvent event : events) {
                try {
                    ContestICalDto dto = parseEvent(event);
                    dto.validate();
                    contests.add(dto);
                } catch (Exception e) {
                    // Log and skip malformed events rather than failing the entire fetch
                    LOG.warn("Skipping malformed contest event: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            LOG.error("Failed to parse iCal data", e);
            throw new ExternalApiException(SOURCE_NAME, "Failed to parse iCal data: " + e.getMessage(), e);
        }

        return contests;
    }

    /**
     * Parse a single VEVENT into a ContestICalDto.
     *
     * @param event the VEvent component
     * @return parsed DTO
     */
    private ContestICalDto parseEvent(VEvent event) {
        // Extract SUMMARY (contest name)
        // Note: event.getProperty() returns Optional<Property> in ical4j 4.x
        String summary = event.getProperty(Property.SUMMARY)
                .map(Summary.class::cast)
                .map(Summary::getValue)
                .orElse(null);

        // Extract DTSTART (start time)
        Instant startTime = event.getProperty(Property.DTSTART)
                .map(DtStart.class::cast)
                .flatMap(dt -> Optional.ofNullable(dt.getDate()))
                .map(temporal -> Instant.from(temporal))
                .orElse(null);

        // Extract DTEND (end time)
        Instant endTime = event.getProperty(Property.DTEND)
                .map(DtEnd.class::cast)
                .flatMap(dt -> Optional.ofNullable(dt.getDate()))
                .map(temporal -> Instant.from(temporal))
                .orElse(null);

        // Extract URL (details page)
        String detailsUrl = event.getProperty(Property.URL)
                .map(Url.class::cast)
                .map(Url::getValue)
                .orElse(null);

        return new ContestICalDto(summary, startTime, endTime, detailsUrl);
    }

    /**
     * Fallback method when WA7BNM service is unavailable.
     * Returns cached data if available.
     */
    @SuppressWarnings("unused")
    private List<ContestICalDto> getCachedContests(Exception e) {
        LOG.warn("Using cached contests due to: {}", e.getMessage());

        var cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            var cached = cache.get("upcoming", List.class);
            if (cached != null && !cached.isEmpty()) {
                LOG.info("Returning {} cached contests", cached.size());
                return cached;
            }
        }

        LOG.error("No cached contests available");
        // Return empty list rather than null to avoid NPE
        return List.of();
    }
}
