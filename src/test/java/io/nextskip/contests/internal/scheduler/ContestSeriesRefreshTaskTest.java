package io.nextskip.contests.internal.scheduler;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.model.FrequencyBand;
import io.nextskip.contests.internal.ContestSeriesClient;
import io.nextskip.contests.internal.dto.ContestSeriesDto;
import io.nextskip.contests.model.Contest;
import io.nextskip.contests.persistence.entity.ContestEntity;
import io.nextskip.contests.persistence.entity.ContestSeriesEntity;
import io.nextskip.contests.persistence.repository.ContestRepository;
import io.nextskip.contests.persistence.repository.ContestSeriesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ContestSeriesRefreshTask.
 */
@ExtendWith(MockitoExtension.class)
class ContestSeriesRefreshTaskTest {

    @Mock
    private ContestSeriesClient seriesClient;

    @Mock
    private ContestRepository contestRepository;

    @Mock
    private ContestSeriesRepository seriesRepository;

    @Mock
    private LoadingCache<String, List<Contest>> contestsCache;

    private ContestSeriesRefreshTask task;

    @BeforeEach
    void setUp() {
        task = new ContestSeriesRefreshTask();
    }

    @Test
    void testContestSeriesRefreshTask_ReturnsValidRecurringTask() {
        RecurringTask<Void> recurringTask = task.contestSeriesRecurringTask(
                seriesClient, contestRepository, seriesRepository, contestsCache, 5);

        assertThat(recurringTask).isNotNull();
        assertThat(recurringTask.getName()).isEqualTo("contest-series-refresh");
    }

    @Test
    void testExecuteRefresh_NewSeries_ScrapesAndSaves() {
        // Given: A contest ref that doesn't exist in series repository
        when(contestRepository.findDistinctWa7bnmRefsByEndTimeAfter(any(Instant.class)))
                .thenReturn(List.of("8"));
        when(seriesRepository.findByWa7bnmRef("8")).thenReturn(Optional.empty());
        when(seriesClient.fetchRevisionDate("8"))
                .thenReturn(Optional.of(LocalDate.of(2025, 11, 1)));
        when(seriesClient.fetchSeriesDetails("8")).thenReturn(createTestSeriesDto());
        when(contestRepository.findByWa7bnmRef("8")).thenReturn(List.of(createTestContestEntity()));

        // When
        task.executeRefresh(seriesClient, contestRepository, seriesRepository, contestsCache, 0);

        // Then: Should scrape and save series
        verify(seriesClient).fetchSeriesDetails("8");
        verify(seriesRepository).save(any(ContestSeriesEntity.class));
        verify(contestRepository).saveAll(any());
        verify(contestsCache).refresh(CacheConfig.CACHE_KEY);
    }

    @Test
    void testExecuteRefresh_ExistingSeriesUnchanged_Skips() {
        // Given: A series that exists with same revision date
        LocalDate revisionDate = LocalDate.of(2025, 11, 1);
        ContestSeriesEntity existingSeries = createTestSeriesEntity(revisionDate);

        when(contestRepository.findDistinctWa7bnmRefsByEndTimeAfter(any(Instant.class)))
                .thenReturn(List.of("8"));
        when(seriesRepository.findByWa7bnmRef("8")).thenReturn(Optional.of(existingSeries));
        when(seriesClient.fetchRevisionDate("8")).thenReturn(Optional.of(revisionDate));

        // When
        task.executeRefresh(seriesClient, contestRepository, seriesRepository, contestsCache, 0);

        // Then: Should skip full scrape
        verify(seriesClient, never()).fetchSeriesDetails(any());
        verify(seriesRepository, never()).save(any());
    }

    @Test
    void testExecuteRefresh_ExistingSeriesChanged_Rescrapes() {
        // Given: A series that exists but has different revision date
        LocalDate oldDate = LocalDate.of(2025, 10, 1);
        LocalDate newDate = LocalDate.of(2025, 11, 1);
        ContestSeriesEntity existingSeries = createTestSeriesEntity(oldDate);

        when(contestRepository.findDistinctWa7bnmRefsByEndTimeAfter(any(Instant.class)))
                .thenReturn(List.of("8"));
        when(seriesRepository.findByWa7bnmRef("8")).thenReturn(Optional.of(existingSeries));
        when(seriesClient.fetchRevisionDate("8")).thenReturn(Optional.of(newDate));
        when(seriesClient.fetchSeriesDetails("8")).thenReturn(createTestSeriesDto());
        when(contestRepository.findByWa7bnmRef("8")).thenReturn(List.of(createTestContestEntity()));

        // When
        task.executeRefresh(seriesClient, contestRepository, seriesRepository, contestsCache, 0);

        // Then: Should rescrape
        verify(seriesClient).fetchSeriesDetails("8");
        verify(seriesRepository).save(any(ContestSeriesEntity.class));
    }

    @Test
    void testExecuteRefresh_NoContestsInWindow_NoScraping() {
        when(contestRepository.findDistinctWa7bnmRefsByEndTimeAfter(any(Instant.class)))
                .thenReturn(List.of());

        task.executeRefresh(seriesClient, contestRepository, seriesRepository, contestsCache, 0);

        verify(seriesClient, never()).fetchRevisionDate(any());
        verify(seriesClient, never()).fetchSeriesDetails(any());
        verify(contestsCache).refresh(CacheConfig.CACHE_KEY);
    }

    @Test
    void testExecuteRefresh_CopiesSeriesDataToContests() {
        // Given
        when(contestRepository.findDistinctWa7bnmRefsByEndTimeAfter(any(Instant.class)))
                .thenReturn(List.of("8"));
        when(seriesRepository.findByWa7bnmRef("8")).thenReturn(Optional.empty());
        when(seriesClient.fetchRevisionDate("8"))
                .thenReturn(Optional.of(LocalDate.of(2025, 11, 1)));
        when(seriesClient.fetchSeriesDetails("8")).thenReturn(createTestSeriesDto());

        ContestEntity contestEntity = createTestContestEntity();
        when(contestRepository.findByWa7bnmRef("8")).thenReturn(List.of(contestEntity));

        // When
        task.executeRefresh(seriesClient, contestRepository, seriesRepository, contestsCache, 0);

        // Then: Contest entity should be updated with series data
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ContestEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(contestRepository).saveAll(captor.capture());

        List<ContestEntity> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getSponsor()).isEqualTo("HDXA");
        assertThat(saved.get(0).getBands()).contains(FrequencyBand.BAND_40M);
        assertThat(saved.get(0).getModes()).contains("CW");
    }

    @Test
    void testExecuteRefresh_SeriesClientError_ContinuesWithOtherRefs() {
        // Given: Two refs, first one fails
        when(contestRepository.findDistinctWa7bnmRefsByEndTimeAfter(any(Instant.class)))
                .thenReturn(List.of("8", "9"));

        // First ref fails
        when(seriesRepository.findByWa7bnmRef("8")).thenReturn(Optional.empty());
        when(seriesClient.fetchRevisionDate("8"))
                .thenThrow(new RuntimeException("Network error"));

        // Second ref succeeds
        when(seriesRepository.findByWa7bnmRef("9")).thenReturn(Optional.empty());
        when(seriesClient.fetchRevisionDate("9"))
                .thenReturn(Optional.of(LocalDate.of(2025, 11, 1)));
        when(seriesClient.fetchSeriesDetails("9")).thenReturn(createTestSeriesDto());
        when(contestRepository.findByWa7bnmRef("9")).thenReturn(List.of());

        // When
        task.executeRefresh(seriesClient, contestRepository, seriesRepository, contestsCache, 0);

        // Then: Should continue processing after error
        verify(seriesClient).fetchSeriesDetails("9");
    }

    @Test
    void testExecuteRefresh_SeriesClientError_AllFail_StillRefreshesCache() {
        // Given: All refs fail
        when(contestRepository.findDistinctWa7bnmRefsByEndTimeAfter(any(Instant.class)))
                .thenReturn(List.of("8"));
        when(seriesRepository.findByWa7bnmRef("8")).thenReturn(Optional.empty());
        when(seriesClient.fetchRevisionDate("8"))
                .thenThrow(new RuntimeException("Network error"));

        // When
        task.executeRefresh(seriesClient, contestRepository, seriesRepository, contestsCache, 0);

        // Then: Cache should still be refreshed
        verify(contestsCache).refresh(CacheConfig.CACHE_KEY);
    }

    private ContestSeriesDto createTestSeriesDto() {
        return new ContestSeriesDto(
                "8",
                "Indiana QSO Party",
                Set.of(FrequencyBand.BAND_40M, FrequencyBand.BAND_20M),
                Set.of("CW", "SSB"),
                "HDXA",
                "https://example.com/rules",
                "RS(T) + county",
                "IN-QSO-PARTY",
                LocalDate.of(2025, 11, 1)
        );
    }

    private ContestSeriesEntity createTestSeriesEntity(LocalDate revisionDate) {
        return new ContestSeriesEntity(
                "8",
                "Indiana QSO Party",
                Set.of(FrequencyBand.BAND_40M),
                Set.of("CW"),
                "HDXA",
                "https://example.com/rules",
                "RS(T) + county",
                "IN-QSO-PARTY",
                revisionDate,
                Instant.now().minus(1, ChronoUnit.DAYS)
        );
    }

    private ContestEntity createTestContestEntity() {
        return new ContestEntity(
                "Indiana QSO Party",
                Instant.now().plus(1, ChronoUnit.DAYS),
                Instant.now().plus(3, ChronoUnit.DAYS),
                Set.of(),
                Set.of(),
                null,
                "https://contestcalendar.com/contestdetails.php?ref=8",
                null
        );
    }
}
