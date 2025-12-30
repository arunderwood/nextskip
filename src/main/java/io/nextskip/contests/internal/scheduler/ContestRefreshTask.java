package io.nextskip.contests.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import io.nextskip.common.scheduler.DataRefreshException;
import io.nextskip.contests.internal.ContestCalendarClient;
import io.nextskip.contests.internal.dto.ContestICalDto;
import io.nextskip.contests.model.Contest;
import io.nextskip.contests.persistence.entity.ContestEntity;
import io.nextskip.contests.persistence.repository.ContestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Recurring task for refreshing contest calendar data.
 *
 * <p>Fetches upcoming amateur radio contests from the WA7BNM Contest Calendar
 * iCal feed and persists them to the database.
 *
 * <p>Task runs every 6 hours (contest schedules change infrequently).
 */
@Configuration
public class ContestRefreshTask {

    private static final Logger LOG = LoggerFactory.getLogger(ContestRefreshTask.class);
    private static final String TASK_NAME = "contest-refresh";
    private static final Duration REFRESH_INTERVAL = Duration.ofHours(6);

    /**
     * Creates the recurring task bean for contest data refresh.
     *
     * @param contestClient the contest calendar API client
     * @param repository    the contest repository for persistence
     * @return the configured recurring task
     */
    @Bean
    public RecurringTask<Void> contestRecurringTask(
            ContestCalendarClient contestClient,
            ContestRepository repository) {

        return Tasks.recurring(TASK_NAME, FixedDelay.of(REFRESH_INTERVAL))
                .execute((taskInstance, executionContext) ->
                        executeRefresh(contestClient, repository));
    }

    /**
     * Executes the contest data refresh.
     *
     * <p>This method is package-private to allow testing.
     *
     * @param contestClient the contest calendar API client
     * @param repository    the contest repository
     */
    @Transactional
    @SuppressWarnings("PMD.AvoidCatchingGenericException") // API client can throw various exceptions
    void executeRefresh(ContestCalendarClient contestClient, ContestRepository repository) {
        LOG.debug("Executing contest refresh task");

        try {
            // Fetch fresh data from API (returns ContestICalDto list)
            List<ContestICalDto> dtos = contestClient.fetch();

            // Convert DTOs to domain model, then to entities
            List<ContestEntity> entities = dtos.stream()
                    .map(this::convertToContest)
                    .map(ContestEntity::fromDomain)
                    .toList();

            // Clear old data and save new (contests are event-based, replace all)
            repository.deleteAll();
            repository.saveAll(entities);

            LOG.info("Contest refresh complete: {} contests saved", entities.size());

        } catch (Exception e) {
            LOG.error("Contest refresh failed: {}", e.getMessage(), e);
            throw new DataRefreshException("Contest refresh failed", e);
        }
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

    /**
     * Checks if initial data load is needed.
     *
     * @param repository the contest repository
     * @return true if no recent contest data exists
     */
    public boolean needsInitialLoad(ContestRepository repository) {
        // Check if we have any contests ending after now
        Instant now = Instant.now();
        return repository.findByEndTimeAfterOrderByStartTimeAsc(now).isEmpty();
    }
}
