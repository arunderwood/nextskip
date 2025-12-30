package io.nextskip.propagation.internal.scheduler;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import io.nextskip.common.config.CacheConfig;
import io.nextskip.common.model.FrequencyBand;
import io.nextskip.propagation.internal.HamQslBandClient;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.BandConditionRating;
import io.nextskip.propagation.persistence.entity.BandConditionEntity;
import io.nextskip.propagation.persistence.repository.BandConditionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for HamQslBandRefreshTask.
 */
@ExtendWith(MockitoExtension.class)
class HamQslBandRefreshTaskTest {

    @Mock
    private HamQslBandClient hamQslBandClient;

    @Mock
    private BandConditionRepository repository;

    @Mock
    private LoadingCache<String, List<BandCondition>> bandConditionsCache;

    private HamQslBandRefreshTask task;

    @BeforeEach
    void setUp() {
        task = new HamQslBandRefreshTask();
    }

    @Test
    void testHamQslBandRefreshTask_ReturnsValidRecurringTask() {
        RecurringTask<Void> recurringTask = task.hamQslBandRecurringTask(
                hamQslBandClient, repository, bandConditionsCache);

        assertThat(recurringTask).isNotNull();
        assertThat(recurringTask.getName()).isEqualTo("hamqsl-band-refresh");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHamQslBandRecurringTask_ExecuteHandler_InvokesRefresh() {
        List<BandCondition> conditions = createTestConditions();
        when(hamQslBandClient.fetch()).thenReturn(conditions);

        RecurringTask<Void> recurringTask = task.hamQslBandRecurringTask(
                hamQslBandClient, repository, bandConditionsCache);
        TaskInstance<Void> taskInstance = (TaskInstance<Void>) org.mockito.Mockito.mock(TaskInstance.class);
        ExecutionContext executionContext = org.mockito.Mockito.mock(ExecutionContext.class);

        recurringTask.execute(taskInstance, executionContext);

        verify(hamQslBandClient).fetch();
    }

    @Test
    void testExecuteRefresh_CallsClientFetch() {
        List<BandCondition> conditions = createTestConditions();
        when(hamQslBandClient.fetch()).thenReturn(conditions);

        task.executeRefresh(hamQslBandClient, repository, bandConditionsCache);

        verify(hamQslBandClient).fetch();
    }

    @Test
    void testExecuteRefresh_CallsRepositorySaveAll() {
        List<BandCondition> conditions = createTestConditions();
        when(hamQslBandClient.fetch()).thenReturn(conditions);

        task.executeRefresh(hamQslBandClient, repository, bandConditionsCache);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BandConditionEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        List<BandConditionEntity> saved = captor.getValue();
        assertThat(saved).hasSize(2);
    }

    @Test
    void testExecuteRefresh_TriggersCacheRefresh() {
        List<BandCondition> conditions = createTestConditions();
        when(hamQslBandClient.fetch()).thenReturn(conditions);

        task.executeRefresh(hamQslBandClient, repository, bandConditionsCache);

        verify(bandConditionsCache).refresh(CacheConfig.CACHE_KEY);
    }

    @Test
    void testExecuteRefresh_ClientThrowsException_PropagatesException() {
        when(hamQslBandClient.fetch()).thenThrow(new RuntimeException("HamQSL Band API error"));

        assertThatThrownBy(() -> task.executeRefresh(hamQslBandClient, repository, bandConditionsCache))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("HamQSL band refresh failed");

        verify(repository, never()).saveAll(any());
    }

    @Test
    void testExecuteRefresh_EmptyList_SavesNothing() {
        when(hamQslBandClient.fetch()).thenReturn(Collections.emptyList());

        task.executeRefresh(hamQslBandClient, repository, bandConditionsCache);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BandConditionEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void testNeedsInitialLoad_EmptyRepository_ReturnsTrue() {
        when(repository.findByRecordedAtAfterOrderByRecordedAtDesc(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        boolean result = task.needsInitialLoad(repository);

        assertThat(result).isTrue();
    }

    @Test
    void testNeedsInitialLoad_HasRecentData_ReturnsFalse() {
        BandConditionEntity entity = createTestEntity();
        when(repository.findByRecordedAtAfterOrderByRecordedAtDesc(any(Instant.class)))
                .thenReturn(List.of(entity));

        boolean result = task.needsInitialLoad(repository);

        assertThat(result).isFalse();
    }

    private List<BandCondition> createTestConditions() {
        return List.of(
                new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD),
                new BandCondition(FrequencyBand.BAND_40M, BandConditionRating.FAIR)
        );
    }

    private BandConditionEntity createTestEntity() {
        return new BandConditionEntity(
                FrequencyBand.BAND_20M,
                BandConditionRating.GOOD,
                1.0,
                null,
                Instant.now()
        );
    }
}
