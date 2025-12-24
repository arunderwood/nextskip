package io.nextskip.contests.internal;

import io.nextskip.contests.api.ContestService;
import io.nextskip.contests.internal.dto.ContestICalDto;
import io.nextskip.contests.model.Contest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Implementation of ContestService.
 *
 * <p>Fetches contest data from the WA7BNM iCal feed and converts it into
 * domain models for the dashboard.
 */
@Service
public class ContestServiceImpl implements ContestService {

    private static final Logger LOG = LoggerFactory.getLogger(ContestServiceImpl.class);

    private final ContestCalendarClient calendarClient;

    @Autowired
    public ContestServiceImpl(ContestCalendarClient calendarClient) {
        this.calendarClient = calendarClient;
    }

    @Override
    public List<Contest> getUpcomingContests() {
        LOG.debug("Fetching upcoming contests");

        // Fetch raw iCal data from WA7BNM
        List<ContestICalDto> dtos = fetchContestDtos();

        // Convert DTOs to domain models
        List<Contest> contests = dtos.stream()
                .map(this::toContest)
                .toList();

        LOG.info("Retrieved {} upcoming contests", contests.size());
        return contests;
    }

    /**
     * Fetch contest DTOs from the calendar client.
     *
     * <p>Primary error handling via ContestCalendarClient's Resilience4j annotations.
     * Service-level fallback ensures dashboard remains functional.
     *
     * @return list of contest DTOs, or empty list on error
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private List<ContestICalDto> fetchContestDtos() {
        try {
            return calendarClient.fetch();
        } catch (RuntimeException e) {
            LOG.error("Failed to fetch contests from calendar: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Convert a contest DTO to a domain model.
     *
     * <p>Since the iCal feed doesn't include metadata like bands, modes, or sponsor,
     * we populate these fields with defaults. Future enhancement: scrape the details
     * page URL to extract this information.
     *
     * @param dto the DTO from iCal parsing
     * @return the domain model
     */
    private Contest toContest(ContestICalDto dto) {
        return new Contest(
                dto.summary(),
                dto.startTime(),
                dto.endTime(),
                Set.of(), // bands - not available in iCal feed (TODO: scrape from details page)
                Set.of(), // modes - not available in iCal feed (TODO: scrape from details page)
                null,     // sponsor - not available in iCal feed (TODO: scrape from details page)
                dto.detailsUrl(), // calendar source URL
                null      // official rules URL - not available in iCal feed (TODO: scrape from details page)
        );
    }
}
