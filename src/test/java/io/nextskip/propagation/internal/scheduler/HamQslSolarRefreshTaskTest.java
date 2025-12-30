package io.nextskip.propagation.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import io.nextskip.propagation.internal.HamQslSolarClient;
import io.nextskip.propagation.model.SolarIndices;
import io.nextskip.propagation.persistence.entity.SolarIndicesEntity;
import io.nextskip.propagation.persistence.repository.SolarIndicesRepository;
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
 * Unit tests for HamQslSolarRefreshTask.
 */
@ExtendWith(MockitoExtension.class)
class HamQslSolarRefreshTaskTest {

    @Mock
    private HamQslSolarClient hamQslSolarClient;

    @Mock
    private SolarIndicesRepository repository;

    private HamQslSolarRefreshTask task;

    @BeforeEach
    void setUp() {
        task = new HamQslSolarRefreshTask();
    }

    @Test
    void testHamQslSolarRefreshTask_ReturnsValidRecurringTask() {
        RecurringTask<Void> recurringTask = task.hamQslSolarRecurringTask(hamQslSolarClient, repository);

        assertThat(recurringTask).isNotNull();
        assertThat(recurringTask.getName()).isEqualTo("hamqsl-solar-refresh");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHamQslSolarRecurringTask_ExecuteHandler_InvokesRefresh() {
        SolarIndices indices = createTestSolarIndices();
        when(hamQslSolarClient.fetch()).thenReturn(indices);

        RecurringTask<Void> recurringTask = task.hamQslSolarRecurringTask(hamQslSolarClient, repository);
        TaskInstance<Void> taskInstance = (TaskInstance<Void>) org.mockito.Mockito.mock(TaskInstance.class);
        ExecutionContext executionContext = org.mockito.Mockito.mock(ExecutionContext.class);

        recurringTask.execute(taskInstance, executionContext);

        verify(hamQslSolarClient).fetch();
    }

    @Test
    void testExecuteRefresh_CallsClientFetch() {
        SolarIndices indices = createTestSolarIndices();
        when(hamQslSolarClient.fetch()).thenReturn(indices);

        task.executeRefresh(hamQslSolarClient, repository);

        verify(hamQslSolarClient).fetch();
    }

    @Test
    void testExecuteRefresh_CallsRepositorySave() {
        SolarIndices indices = createTestSolarIndices();
        when(hamQslSolarClient.fetch()).thenReturn(indices);

        task.executeRefresh(hamQslSolarClient, repository);

        ArgumentCaptor<SolarIndicesEntity> captor = ArgumentCaptor.forClass(SolarIndicesEntity.class);
        verify(repository).save(captor.capture());

        SolarIndicesEntity saved = captor.getValue();
        assertThat(saved.getKIndex()).isEqualTo(4);
        assertThat(saved.getAIndex()).isEqualTo(12);
    }

    @Test
    void testExecuteRefresh_ClientReturnsNull_SkipsSave() {
        when(hamQslSolarClient.fetch()).thenReturn(null);

        task.executeRefresh(hamQslSolarClient, repository);

        verify(repository, never()).save(any());
    }

    @Test
    void testExecuteRefresh_ClientThrowsException_PropagatesException() {
        when(hamQslSolarClient.fetch()).thenThrow(new RuntimeException("HamQSL API error"));

        assertThatThrownBy(() -> task.executeRefresh(hamQslSolarClient, repository))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("HamQSL solar refresh failed");

        verify(repository, never()).save(any());
    }

    @Test
    void testNeedsInitialLoad_EmptyRepository_ReturnsTrue() {
        when(repository.findByTimestampAfterOrderByTimestampDesc(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        boolean result = task.needsInitialLoad(repository);

        assertThat(result).isTrue();
    }

    @Test
    void testNeedsInitialLoad_HasHamQslData_ReturnsFalse() {
        SolarIndicesEntity entity = createTestEntity("HamQSL");
        when(repository.findByTimestampAfterOrderByTimestampDesc(any(Instant.class)))
                .thenReturn(List.of(entity));

        boolean result = task.needsInitialLoad(repository);

        assertThat(result).isFalse();
    }

    @Test
    void testNeedsInitialLoad_OnlyNoaaData_ReturnsTrue() {
        SolarIndicesEntity entity = createTestEntity("NOAA");
        when(repository.findByTimestampAfterOrderByTimestampDesc(any(Instant.class)))
                .thenReturn(List.of(entity));

        boolean result = task.needsInitialLoad(repository);

        assertThat(result).isTrue();
    }

    private SolarIndices createTestSolarIndices() {
        return new SolarIndices(
                145.0,
                12,
                4,
                95,
                Instant.now(),
                "HamQSL"
        );
    }

    private SolarIndicesEntity createTestEntity(String source) {
        return new SolarIndicesEntity(
                145.0,
                12,
                4,
                95,
                Instant.now(),
                source
        );
    }
}
