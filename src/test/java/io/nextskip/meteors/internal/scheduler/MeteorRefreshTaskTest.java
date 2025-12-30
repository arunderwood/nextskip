package io.nextskip.meteors.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import io.nextskip.meteors.internal.MeteorShowerDataLoader;
import io.nextskip.meteors.model.MeteorShower;
import io.nextskip.meteors.persistence.entity.MeteorShowerEntity;
import io.nextskip.meteors.persistence.repository.MeteorShowerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MeteorRefreshTask.
 */
@ExtendWith(MockitoExtension.class)
class MeteorRefreshTaskTest {

    @Mock
    private MeteorShowerDataLoader dataLoader;

    @Mock
    private MeteorShowerRepository repository;

    private MeteorRefreshTask task;

    @BeforeEach
    void setUp() {
        task = new MeteorRefreshTask();
    }

    @Test
    void testMeteorRefreshTask_ReturnsValidRecurringTask() {
        RecurringTask<Void> recurringTask = task.meteorRecurringTask(dataLoader, repository);

        assertThat(recurringTask).isNotNull();
        assertThat(recurringTask.getName()).isEqualTo("meteor-refresh");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testMeteorRecurringTask_ExecuteHandler_InvokesRefresh() {
        List<MeteorShower> showers = createTestShowers();
        when(dataLoader.getShowers(anyInt())).thenReturn(showers);

        RecurringTask<Void> recurringTask = task.meteorRecurringTask(dataLoader, repository);
        TaskInstance<Void> taskInstance = (TaskInstance<Void>) org.mockito.Mockito.mock(TaskInstance.class);
        ExecutionContext executionContext = org.mockito.Mockito.mock(ExecutionContext.class);

        recurringTask.execute(taskInstance, executionContext);

        verify(dataLoader).getShowers(30);
    }

    @Test
    void testExecuteRefresh_CallsDataLoader() {
        List<MeteorShower> showers = createTestShowers();
        when(dataLoader.getShowers(anyInt())).thenReturn(showers);

        task.executeRefresh(dataLoader, repository);

        verify(dataLoader).getShowers(30);
    }

    @Test
    void testExecuteRefresh_CallsRepositoryDeleteAll() {
        List<MeteorShower> showers = createTestShowers();
        when(dataLoader.getShowers(anyInt())).thenReturn(showers);

        task.executeRefresh(dataLoader, repository);

        verify(repository).deleteAll();
    }

    @Test
    void testExecuteRefresh_CallsRepositorySaveAll() {
        List<MeteorShower> showers = createTestShowers();
        when(dataLoader.getShowers(anyInt())).thenReturn(showers);

        task.executeRefresh(dataLoader, repository);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MeteorShowerEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        List<MeteorShowerEntity> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getName()).isEqualTo("Perseids 2025");
    }

    @Test
    void testExecuteRefresh_DataLoaderThrowsException_PropagatesException() {
        when(dataLoader.getShowers(anyInt())).thenThrow(new RuntimeException("Data loader error"));

        assertThatThrownBy(() -> task.executeRefresh(dataLoader, repository))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Meteor refresh failed");

        verify(repository, never()).deleteAll();
        verify(repository, never()).saveAll(any());
    }

    @Test
    void testExecuteRefresh_EmptyList_DeletesAllAndSavesNothing() {
        when(dataLoader.getShowers(anyInt())).thenReturn(Collections.emptyList());

        task.executeRefresh(dataLoader, repository);

        verify(repository).deleteAll();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MeteorShowerEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void testNeedsInitialLoad_NoActiveOrUpcoming_ReturnsTrue() {
        when(repository.findByVisibilityStartBeforeAndVisibilityEndAfterOrderByPeakStartAsc(
                any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(repository.findByVisibilityStartAfterOrderByVisibilityStartAsc(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        boolean result = task.needsInitialLoad(repository);

        assertThat(result).isTrue();
    }

    @Test
    void testNeedsInitialLoad_HasActiveShower_ReturnsFalse() {
        MeteorShowerEntity entity = createTestEntity();
        when(repository.findByVisibilityStartBeforeAndVisibilityEndAfterOrderByPeakStartAsc(
                any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(entity));

        boolean result = task.needsInitialLoad(repository);

        assertThat(result).isFalse();
    }

    @Test
    void testNeedsInitialLoad_HasUpcomingShower_ReturnsFalse() {
        when(repository.findByVisibilityStartBeforeAndVisibilityEndAfterOrderByPeakStartAsc(
                any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        MeteorShowerEntity entity = createTestEntity();
        when(repository.findByVisibilityStartAfterOrderByVisibilityStartAsc(any(Instant.class)))
                .thenReturn(List.of(entity));

        boolean result = task.needsInitialLoad(repository);

        assertThat(result).isFalse();
    }

    private List<MeteorShower> createTestShowers() {
        Instant now = Instant.now();
        return List.of(new MeteorShower(
                "Perseids 2025",
                "PER",
                now.plus(10, ChronoUnit.DAYS),
                now.plus(11, ChronoUnit.DAYS),
                now.plus(5, ChronoUnit.DAYS),
                now.plus(20, ChronoUnit.DAYS),
                100,
                "109P/Swift-Tuttle",
                "https://example.com/perseids"
        ));
    }

    private MeteorShowerEntity createTestEntity() {
        Instant now = Instant.now();
        return new MeteorShowerEntity(
                "Perseids 2025",
                "PER",
                now.plus(10, ChronoUnit.DAYS),
                now.plus(11, ChronoUnit.DAYS),
                now.minus(5, ChronoUnit.DAYS),
                now.plus(20, ChronoUnit.DAYS),
                100,
                "109P/Swift-Tuttle",
                "https://example.com/perseids"
        );
    }
}
