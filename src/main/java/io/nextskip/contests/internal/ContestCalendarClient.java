package io.nextskip.contests.internal;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.nextskip.common.client.AbstractExternalDataClient;
import io.nextskip.common.client.ExternalApiException;
import io.nextskip.contests.internal.dto.ContestICalDto;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Url;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

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
 * <p>Extends {@link AbstractExternalDataClient} to inherit:
 * <ul>
 *   <li>Circuit breaker to prevent cascading failures</li>
 *   <li>Retry logic for transient failures</li>
 *   <li>Freshness tracking for UI display</li>
 * </ul>
 */
@Component
@SuppressWarnings("PMD.AvoidCatchingGenericException") // Intentional: wrap parsing exceptions
public class ContestCalendarClient extends AbstractExternalDataClient<List<ContestICalDto>> {

    private static final String CLIENT_NAME = "contests";
    private static final String SOURCE_NAME = "WA7BNM Contest Calendar";

    /**
     * Refresh interval for data fetching.
     *
     * <p>Contest schedules are static and change infrequently. 6 hours is
     * appropriate for catching schedule updates while minimizing load.
     *
     * @see <a href="https://www.contestcalendar.com/terms.php">WA7BNM Terms of Use</a>
     */
    private static final Duration REFRESH_INTERVAL = Duration.ofHours(6);
    private static final String CALENDAR_URL = "https://www.contestcalendar.com/weeklycontcustom.php";

    @org.springframework.beans.factory.annotation.Autowired
    public ContestCalendarClient(
            WebClient.Builder webClientBuilder,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry) {
        this(webClientBuilder, circuitBreakerRegistry, retryRegistry, CALENDAR_URL);
    }

    protected ContestCalendarClient(
            WebClient.Builder webClientBuilder,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            String baseUrl) {
        super(webClientBuilder, circuitBreakerRegistry, retryRegistry, baseUrl);
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
    protected Duration getRequestTimeout() {
        return Duration.ofSeconds(15); // Longer timeout for contest calendar
    }

    @Override
    protected List<ContestICalDto> doFetch() {
        getLog().debug("Fetching contests from WA7BNM iCal feed");

        // Fetch iCal data as string
        String icalData = getWebClient().get()
                .retrieve()
                .bodyToMono(String.class)
                .timeout(getRequestTimeout())
                .block();

        if (icalData == null || icalData.isBlank()) {
            getLog().warn("No data received from WA7BNM contest calendar");
            throw new ExternalApiException(CLIENT_NAME, "Empty response from contest calendar");
        }

        // Parse iCal using ical4j
        List<ContestICalDto> contests = parseICalData(icalData);

        getLog().info("Successfully fetched {} contests from WA7BNM", contests.size());
        return contests;
    }

    // ========== iCal parsing ==========

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
                    getLog().warn("Skipping malformed contest event: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            getLog().error("Failed to parse iCal data", e);
            throw new ExternalApiException(CLIENT_NAME, "Failed to parse iCal data: " + e.getMessage(), e);
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
        String summary = event.getProperty(Property.SUMMARY)
                .map(Summary.class::cast)
                .map(Summary::getValue)
                .orElse(null);

        // Extract DTSTART (start time)
        Instant startTime = event.getProperty(Property.DTSTART)
                .map(DtStart.class::cast)
                .flatMap(dt -> Optional.ofNullable(dt.getDate()))
                .map(Instant::from)
                .orElse(null);

        // Extract DTEND (end time)
        Instant endTime = event.getProperty(Property.DTEND)
                .map(DtEnd.class::cast)
                .flatMap(dt -> Optional.ofNullable(dt.getDate()))
                .map(Instant::from)
                .orElse(null);

        // Extract URL (details page)
        String detailsUrl = event.getProperty(Property.URL)
                .map(Url.class::cast)
                .map(Url::getValue)
                .orElse(null);

        return new ContestICalDto(summary, startTime, endTime, detailsUrl);
    }
}
