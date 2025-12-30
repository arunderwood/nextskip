package io.nextskip.propagation.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import io.nextskip.propagation.internal.NoaaSwpcClient;
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
 * Unit tests for NoaaRefreshTask.
 */
@ExtendWith(MockitoExtension.class)
class NoaaRefreshTaskTest {

    @Mock
    private NoaaSwpcClient noaaClient;

    @Mock
    private SolarIndicesRepository repository;

    private NoaaRefreshTask task;

    @BeforeEach
    void setUp() {
        task = new NoaaRefreshTask();
    }

    @Test
    void testNoaaRefreshTask_ReturnsValidRecurringTask() {
        RecurringTask<Void> recurringTask = task.noaaRecurringTask(noaaClient, repository);

        assertThat(recurringTask).isNotNull();
        assertThat(recurringTask.getName()).isEqualTo("noaa-refresh");
    }

    @Test
    void testExecuteRefresh_CallsClientFetch() {
        SolarIndices indices = createTestSolarIndices();
        when(noaaClient.fetch()).thenReturn(indices);

        task.executeRefresh(noaaClient, repository);

        verify(noaaClient).fetch();
    }

    @Test
    void testExecuteRefresh_CallsRepositorySave() {
        SolarIndices indices = createTestSolarIndices();
        when(noaaClient.fetch()).thenReturn(indices);

        task.executeRefresh(noaaClient, repository);

        ArgumentCaptor<SolarIndicesEntity> captor = ArgumentCaptor.forClass(SolarIndicesEntity.class);
        verify(repository).save(captor.capture());

        SolarIndicesEntity saved = captor.getValue();
        assertThat(saved.getSolarFluxIndex()).isEqualTo(150.0);
        assertThat(saved.getSunspotNumber()).isEqualTo(100);
    }

    @Test
    void testExecuteRefresh_ClientThrowsException_PropagatesException() {
        when(noaaClient.fetch()).thenThrow(new RuntimeException("NOAA API error"));

        assertThatThrownBy(() -> task.executeRefresh(noaaClient, repository))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("NOAA refresh failed");

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
    void testNeedsInitialLoad_HasNoaaData_ReturnsFalse() {
        SolarIndicesEntity entity = createTestEntity("NOAA");
        when(repository.findByTimestampAfterOrderByTimestampDesc(any(Instant.class)))
                .thenReturn(List.of(entity));

        boolean result = task.needsInitialLoad(repository);

        assertThat(result).isFalse();
    }

    @Test
    void testNeedsInitialLoad_OnlyHamQslData_ReturnsTrue() {
        SolarIndicesEntity entity = createTestEntity("HamQSL");
        when(repository.findByTimestampAfterOrderByTimestampDesc(any(Instant.class)))
                .thenReturn(List.of(entity));

        boolean result = task.needsInitialLoad(repository);

        assertThat(result).isTrue();
    }

    private SolarIndices createTestSolarIndices() {
        return new SolarIndices(
                150.0,
                10,
                3,
                100,
                Instant.now(),
                "NOAA SWPC"
        );
    }

    private SolarIndicesEntity createTestEntity(String source) {
        return new SolarIndicesEntity(
                150.0,
                10,
                3,
                100,
                Instant.now(),
                source
        );
    }
}
