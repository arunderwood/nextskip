package io.nextskip.propagation.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import io.nextskip.common.model.FrequencyBand;
import io.nextskip.propagation.model.BandCondition;
import io.nextskip.propagation.model.BandConditionRating;
import io.nextskip.propagation.persistence.entity.BandConditionEntity;
import io.nextskip.propagation.persistence.entity.SolarIndicesEntity;
import io.nextskip.propagation.persistence.repository.BandConditionRepository;
import io.nextskip.propagation.persistence.repository.SolarIndicesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for unified HamQslRefreshTask.
 *
 * <p>Tests the task configuration, initial load detection, and RefreshTaskCoordinator
 * implementation. Business logic tests are in {@link HamQslRefreshServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class HamQslRefreshTaskTest {

    @Mock
    private HamQslRefreshService refreshService;

    @Mock
    private SolarIndicesRepository solarRepository;

    @Mock
    private BandConditionRepository bandRepository;

    private HamQslRefreshTask task;

    @BeforeEach
    void setUp() {
        task = new HamQslRefreshTask(solarRepository, bandRepository);
    }

    @Test
    void testHamQslRefreshTask_ReturnsValidRecurringTask() {
        RecurringTask<Void> recurringTask = task.hamQslRecurringTask(refreshService);

        assertThat(recurringTask).isNotNull();
        assertThat(recurringTask.getName()).isEqualTo("hamqsl-refresh");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHamQslRecurringTask_ExecuteHandler_DelegatesToService() {
        RecurringTask<Void> recurringTask = task.hamQslRecurringTask(refreshService);
        TaskInstance<Void> taskInstance = (TaskInstance<Void>) org.mockito.Mockito.mock(TaskInstance.class);
        ExecutionContext executionContext = org.mockito.Mockito.mock(ExecutionContext.class);

        recurringTask.execute(taskInstance, executionContext);

        verify(refreshService).executeRefresh();
    }

    @Test
    void testNeedsInitialLoad_EmptyRepositories_ReturnsTrue() {
        when(solarRepository.findByTimestampAfterOrderByTimestampDesc(any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(bandRepository.findByRecordedAtAfterOrderByRecordedAtDesc(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        boolean result = task.needsInitialLoad();

        assertThat(result).isTrue();
    }

    @Test
    void testNeedsInitialLoad_HasHamQslSolarDataOnly_ReturnsTrue() {
        SolarIndicesEntity entity = createTestSolarEntity("HamQSL");
        when(solarRepository.findByTimestampAfterOrderByTimestampDesc(any(Instant.class)))
                .thenReturn(List.of(entity));
        when(bandRepository.findByRecordedAtAfterOrderByRecordedAtDesc(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        boolean result = task.needsInitialLoad();

        // Needs initial load because band data is missing
        assertThat(result).isTrue();
    }

    @Test
    void testNeedsInitialLoad_HasBandDataOnly_ReturnsTrue() {
        when(solarRepository.findByTimestampAfterOrderByTimestampDesc(any(Instant.class)))
                .thenReturn(Collections.emptyList());
        BandConditionEntity bandEntity = createTestBandEntity();
        when(bandRepository.findByRecordedAtAfterOrderByRecordedAtDesc(any(Instant.class)))
                .thenReturn(List.of(bandEntity));

        boolean result = task.needsInitialLoad();

        // Needs initial load because HamQSL solar data is missing
        assertThat(result).isTrue();
    }

    @Test
    void testNeedsInitialLoad_HasBothDataTypes_ReturnsFalse() {
        SolarIndicesEntity solarEntity = createTestSolarEntity("HamQSL");
        when(solarRepository.findByTimestampAfterOrderByTimestampDesc(any(Instant.class)))
                .thenReturn(List.of(solarEntity));
        BandConditionEntity bandEntity = createTestBandEntity();
        when(bandRepository.findByRecordedAtAfterOrderByRecordedAtDesc(any(Instant.class)))
                .thenReturn(List.of(bandEntity));

        boolean result = task.needsInitialLoad();

        assertThat(result).isFalse();
    }

    @Test
    void testNeedsInitialLoad_HasOnlyNoaaSolarData_ReturnsTrue() {
        // Having NOAA data doesn't satisfy HamQSL requirement
        SolarIndicesEntity entity = createTestSolarEntity("NOAA SWPC");
        when(solarRepository.findByTimestampAfterOrderByTimestampDesc(any(Instant.class)))
                .thenReturn(List.of(entity));
        when(bandRepository.findByRecordedAtAfterOrderByRecordedAtDesc(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        boolean result = task.needsInitialLoad();

        assertThat(result).isTrue();
    }

    @Test
    void testGetTaskName_ReturnsHamQSL() {
        assertThat(task.getTaskName()).isEqualTo("HamQSL");
    }

    @Test
    void testGetRecurringTask_AfterSetterCalled_ReturnsTask() {
        RecurringTask<Void> recurringTask = task.hamQslRecurringTask(refreshService);
        task.setRecurringTask(recurringTask);

        assertThat(task.getRecurringTask()).isSameAs(recurringTask);
    }

    private SolarIndicesEntity createTestSolarEntity(String source) {
        return new SolarIndicesEntity(
                145.5,
                8,
                3,
                115,
                Instant.now(),
                source
        );
    }

    private BandConditionEntity createTestBandEntity() {
        BandCondition domain = new BandCondition(FrequencyBand.BAND_20M, BandConditionRating.GOOD);
        return BandConditionEntity.fromDomain(domain, Instant.now());
    }
}
