package io.nextskip.contests.internal.scheduler;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Daily;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.scheduler.DataRefreshException;
import io.nextskip.contests.internal.ContestSeriesClient;
import io.nextskip.contests.internal.dto.ContestSeriesDto;
import io.nextskip.contests.model.Contest;
import io.nextskip.contests.persistence.entity.ContestEntity;
import io.nextskip.contests.persistence.entity.ContestSeriesEntity;
import io.nextskip.contests.persistence.repository.ContestRepository;
import io.nextskip.contests.persistence.repository.ContestSeriesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Recurring task for enriching contests with series metadata from WA7BNM detail pages.
 *
 * <p>Scrapes contest detail pages from WA7BNM Contest Calendar to populate
 * bands, modes, sponsor, official rules URL, and other metadata. Uses a
 * ContestSeries entity to cache scraped data and avoid redundant scraping.
 *
 * <p>The task runs daily at 4am UTC (off-peak hours). For each unique series
 * reference in upcoming contests, it:
 * <ol>
 *   <li>Checks if the series already exists and if revision date has changed</li>
 *   <li>If changed or new, scrapes the full page and saves the series</li>
 *   <li>Copies series metadata to all contest occurrences with that reference</li>
 * </ol>
 *
 * <p>Rate limiting is applied between requests to respect the source website.
 */
@Configuration
public class ContestSeriesRefreshTask {

    private static final Logger LOG = LoggerFactory.getLogger(ContestSeriesRefreshTask.class);
    private static final String TASK_NAME = "contest-series-refresh";

    /**
     * Creates the recurring task bean for contest series data refresh.
     *
     * @param seriesClient      the contest series scraper client
     * @param contestRepository the contest repository
     * @param seriesRepository  the contest series repository
     * @param contestsCache     the LoadingCache to refresh after updates
     * @param rateLimitSeconds  seconds to wait between scrape requests
     * @return the configured recurring task
     */
    @Bean
    public RecurringTask<Void> contestSeriesRecurringTask(
            ContestSeriesClient seriesClient,
            ContestRepository contestRepository,
            ContestSeriesRepository seriesRepository,
            LoadingCache<String, List<Contest>> contestsCache,
            @Value("${nextskip.contests.series.rate-limit-seconds:5}") int rateLimitSeconds) {

        return Tasks.recurring(TASK_NAME, new Daily(ZoneOffset.UTC, LocalTime.of(4, 0)))
                .execute((taskInstance, executionContext) ->
                        executeRefresh(seriesClient, contestRepository, seriesRepository,
                                contestsCache, rateLimitSeconds));
    }

    /**
     * Executes the contest series data refresh.
     *
     * <p>For each unique series in upcoming contests, checks if the series
     * needs to be scraped (new or revision date changed), scrapes if needed,
     * and copies metadata to contest occurrences.
     *
     * <p>This method is package-private to allow testing.
     *
     * @param seriesClient      the contest series scraper client
     * @param contestRepository the contest repository
     * @param seriesRepository  the contest series repository
     * @param contestsCache     the cache to refresh
     * @param rateLimitSeconds  seconds to wait between requests
     */
    @Transactional
    @SuppressWarnings("PMD.AvoidCatchingGenericException") // API client can throw various exceptions
    void executeRefresh(
            ContestSeriesClient seriesClient,
            ContestRepository contestRepository,
            ContestSeriesRepository seriesRepository,
            LoadingCache<String, List<Contest>> contestsCache,
            int rateLimitSeconds) {

        LOG.info("Starting contest series refresh task");

        try {
            // Find unique series references for contests ending after now
            List<String> refs = contestRepository.findDistinctWa7bnmRefsByEndTimeAfter(Instant.now());

            LOG.debug("Found {} unique series references to process", refs.size());

            int scraped = 0;
            int skipped = 0;
            int errors = 0;

            for (String ref : refs) {
                try {
                    boolean wasScraped = processSeriesRef(ref, seriesClient, contestRepository,
                            seriesRepository);
                    if (wasScraped) {
                        scraped++;
                        // Rate limit between scrapes
                        sleepBetweenRequests(rateLimitSeconds);
                    } else {
                        skipped++;
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to process series ref={}: {}", ref, e.getMessage());
                    errors++;
                }
            }

            // Trigger async cache refresh
            contestsCache.refresh(CacheConfig.CACHE_KEY);

            LOG.info("Contest series refresh complete: scraped={}, skipped={}, errors={}",
                    scraped, skipped, errors);

        } catch (Exception e) {
            LOG.error("Contest series refresh failed: {}", e.getMessage(), e);
            throw new DataRefreshException("Contest series refresh failed", e);
        }
    }

    /**
     * Processes a single series reference.
     *
     * <p>Checks if the series needs to be scraped by comparing revision dates.
     * If scraping is needed, fetches the full page, saves the series, and
     * copies metadata to all contest occurrences.
     *
     * @param ref               the WA7BNM reference
     * @param seriesClient      the scraper client
     * @param contestRepository the contest repository
     * @param seriesRepository  the series repository
     * @return true if the series was scraped, false if skipped
     */
    private boolean processSeriesRef(
            String ref,
            ContestSeriesClient seriesClient,
            ContestRepository contestRepository,
            ContestSeriesRepository seriesRepository) {

        LOG.debug("Processing series ref={}", ref);

        // Check existing series for change detection
        Optional<ContestSeriesEntity> existing = seriesRepository.findByWa7bnmRef(ref);
        LocalDate existingRevisionDate = existing.map(ContestSeriesEntity::getRevisionDate).orElse(null);

        // Fetch current revision date from page (lightweight check)
        Optional<LocalDate> currentRevisionDate = seriesClient.fetchRevisionDate(ref);

        // Skip if revision date unchanged
        if (existingRevisionDate != null && currentRevisionDate.isPresent()
                && existingRevisionDate.equals(currentRevisionDate.get())) {
            LOG.debug("Series ref={} unchanged (revision date: {})", ref, existingRevisionDate);
            return false;
        }

        // Need to scrape - fetch full details
        LOG.debug("Scraping series ref={} (revision date changed: {} -> {})",
                ref, existingRevisionDate, currentRevisionDate.orElse(null));

        ContestSeriesDto dto = seriesClient.fetchSeriesDetails(ref);

        // Save or update series entity
        ContestSeriesEntity entity = existing.orElseGet(ContestSeriesRefreshTask::createNewSeriesEntity);
        updateSeriesEntity(entity, dto);
        seriesRepository.save(entity);

        // Copy metadata to all contest occurrences
        copySeriesDataToContests(ref, entity, contestRepository);

        LOG.debug("Scraped and saved series ref={}", ref);
        return true;
    }

    /**
     * Factory method to create a new ContestSeriesEntity.
     * Required because the default constructor is protected (JPA-only).
     */
    private static ContestSeriesEntity createNewSeriesEntity() {
        return new ContestSeriesEntity(
                null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Updates a ContestSeriesEntity from a DTO.
     */
    private void updateSeriesEntity(ContestSeriesEntity entity, ContestSeriesDto dto) {
        entity.setWa7bnmRef(dto.wa7bnmRef());
        entity.setName(dto.name());
        entity.setBands(dto.bands());
        entity.setModes(dto.modes());
        entity.setSponsor(dto.sponsor());
        entity.setOfficialRulesUrl(dto.officialRulesUrl());
        entity.setExchange(dto.exchange());
        entity.setCabrilloName(dto.cabrilloName());
        entity.setRevisionDate(dto.revisionDate());
        entity.setLastScrapedAt(Instant.now());
    }

    /**
     * Copies series metadata to all contest entities with the given reference.
     */
    private void copySeriesDataToContests(String ref, ContestSeriesEntity series,
                                          ContestRepository contestRepository) {
        List<ContestEntity> contests = contestRepository.findByWa7bnmRef(ref);

        for (ContestEntity contest : contests) {
            contest.setBands(series.getBands());
            contest.setModes(series.getModes());
            contest.setSponsor(series.getSponsor());
            contest.setOfficialRulesUrl(series.getOfficialRulesUrl());
        }

        contestRepository.saveAll(contests);
        LOG.debug("Copied series data to {} contest occurrences for ref={}", contests.size(), ref);
    }

    /**
     * Sleeps between requests for rate limiting.
     */
    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes") // InterruptedException requires re-interrupt
    private void sleepBetweenRequests(int seconds) {
        try {
            Thread.sleep(Duration.ofSeconds(seconds).toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during rate limit sleep", e);
        }
    }
}
