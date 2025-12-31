package io.nextskip.contests.internal.scheduler;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.scheduler.AbstractRefreshService;
import io.nextskip.common.scheduler.DataRefreshException;
import io.nextskip.contests.internal.ContestCalendarClient;
import io.nextskip.contests.internal.dto.ContestICalDto;
import io.nextskip.contests.model.Contest;
import io.nextskip.contests.persistence.entity.ContestEntity;
import io.nextskip.contests.persistence.repository.ContestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Service for refreshing contest calendar data.
 *
 * <p>Handles the transactional business logic for fetching upcoming amateur radio
 * contests from the WA7BNM Contest Calendar iCal feed, persisting them to the
 * database, and triggering cache refresh.
 *
 * <p>Extends {@link AbstractRefreshService} to inherit transaction management
 * and consistent logging patterns.
 */
@Service
public class ContestRefreshService extends AbstractRefreshService {

    private static final Logger LOG = LoggerFactory.getLogger(ContestRefreshService.class);
    private static final String SERVICE_NAME = "Contest";

    private final ContestCalendarClient contestClient;
    private final ContestRepository repository;
    private final LoadingCache<String, List<Contest>> contestsCache;

    // Metrics for success message
    private int savedCount;

    public ContestRefreshService(
            ContestCalendarClient contestClient,
            ContestRepository repository,
            LoadingCache<String, List<Contest>> contestsCache) {
        this.contestClient = contestClient;
        this.repository = repository;
        this.contestsCache = contestsCache;
    }

    @Override
    protected String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected void doRefresh() {
        // Fetch fresh data from API (returns ContestICalDto list)
        List<ContestICalDto> dtos = contestClient.fetch();

        // Convert DTOs to domain model, then to entities
        List<ContestEntity> entities = dtos.stream()
                .map(this::convertToContest)
                .map(ContestEntity::fromDomain)
                .toList();

        try {
            // Clear old data and save new (contests are event-based, replace all)
            repository.deleteAll();
            repository.saveAll(entities);
            this.savedCount = entities.size();

        } catch (DataAccessException e) {
            throw new DataRefreshException("Database error during contest refresh", e);
        }
    }

    @Override
    protected void refreshCache() {
        contestsCache.refresh(CacheConfig.CACHE_KEY);
    }

    @Override
    protected String getSuccessMessage() {
        return String.format("Contest refresh complete: %d contests saved", savedCount);
    }

    @Override
    protected Logger getLog() {
        return LOG;
    }

    /**
     * Converts a ContestICalDto to a Contest domain model.
     *
     * <p>Since the iCal feed doesn't include band/mode information,
     * these are set to empty sets. The calendar source URL is preserved.
     *
     * @param dto the iCal DTO to convert
     * @return the contest domain model
     */
    private Contest convertToContest(ContestICalDto dto) {
        return new Contest(
                dto.summary(),
                dto.startTime(),
                dto.endTime(),
                Set.of(),           // bands not available from iCal
                Set.of(),           // modes not available from iCal
                null,               // sponsor not available from iCal
                dto.detailsUrl(),   // calendarSourceUrl
                null                // officialRulesUrl not available from iCal
        );
    }
}
