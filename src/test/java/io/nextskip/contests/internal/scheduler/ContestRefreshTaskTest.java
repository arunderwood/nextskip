package io.nextskip.contests.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import io.nextskip.contests.internal.ContestCalendarClient;
import io.nextskip.contests.internal.dto.ContestICalDto;
import io.nextskip.contests.persistence.entity.ContestEntity;
import io.nextskip.contests.persistence.repository.ContestRepository;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ContestRefreshTask.
 */
@ExtendWith(MockitoExtension.class)
class ContestRefreshTaskTest {

    @Mock
    private ContestCalendarClient contestClient;

    @Mock
    private ContestRepository repository;

    private ContestRefreshTask task;

    @BeforeEach
    void setUp() {
        task = new ContestRefreshTask();
    }

    @Test
    void testContestRefreshTask_ReturnsValidRecurringTask() {
        RecurringTask<Void> recurringTask = task.contestRecurringTask(contestClient, repository);

        assertThat(recurringTask).isNotNull();
        assertThat(recurringTask.getName()).isEqualTo("contest-refresh");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testContestRecurringTask_ExecuteHandler_InvokesRefresh() {
        List<ContestICalDto> dtos = createTestDtos();
        when(contestClient.fetch()).thenReturn(dtos);

        RecurringTask<Void> recurringTask = task.contestRecurringTask(contestClient, repository);
        TaskInstance<Void> taskInstance = (TaskInstance<Void>) org.mockito.Mockito.mock(TaskInstance.class);
        ExecutionContext executionContext = org.mockito.Mockito.mock(ExecutionContext.class);

        recurringTask.execute(taskInstance, executionContext);

        verify(contestClient).fetch();
    }

    @Test
    void testExecuteRefresh_CallsClientFetch() {
        List<ContestICalDto> dtos = createTestDtos();
        when(contestClient.fetch()).thenReturn(dtos);

        task.executeRefresh(contestClient, repository);

        verify(contestClient).fetch();
    }

    @Test
    void testExecuteRefresh_CallsRepositoryDeleteAll() {
        List<ContestICalDto> dtos = createTestDtos();
        when(contestClient.fetch()).thenReturn(dtos);

        task.executeRefresh(contestClient, repository);

        verify(repository).deleteAll();
    }

    @Test
    void testExecuteRefresh_CallsRepositorySaveAll() {
        List<ContestICalDto> dtos = createTestDtos();
        when(contestClient.fetch()).thenReturn(dtos);

        task.executeRefresh(contestClient, repository);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ContestEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        List<ContestEntity> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getName()).isEqualTo("CQ WW DX Contest");
    }

    @Test
    void testExecuteRefresh_ClientThrowsException_PropagatesException() {
        when(contestClient.fetch()).thenThrow(new RuntimeException("Contest API error"));

        assertThatThrownBy(() -> task.executeRefresh(contestClient, repository))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Contest refresh failed");

        verify(repository, never()).deleteAll();
        verify(repository, never()).saveAll(any());
    }

    @Test
    void testExecuteRefresh_EmptyList_DeletesAllAndSavesNothing() {
        when(contestClient.fetch()).thenReturn(Collections.emptyList());

        task.executeRefresh(contestClient, repository);

        verify(repository).deleteAll();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ContestEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void testNeedsInitialLoad_EmptyRepository_ReturnsTrue() {
        when(repository.findByEndTimeAfterOrderByStartTimeAsc(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        boolean result = task.needsInitialLoad(repository);

        assertThat(result).isTrue();
    }

    @Test
    void testNeedsInitialLoad_HasFutureContests_ReturnsFalse() {
        ContestEntity entity = createTestEntity();
        when(repository.findByEndTimeAfterOrderByStartTimeAsc(any(Instant.class)))
                .thenReturn(List.of(entity));

        boolean result = task.needsInitialLoad(repository);

        assertThat(result).isFalse();
    }

    private List<ContestICalDto> createTestDtos() {
        Instant now = Instant.now();
        return List.of(
                new ContestICalDto(
                        "CQ WW DX Contest",
                        now.plus(1, ChronoUnit.DAYS),
                        now.plus(3, ChronoUnit.DAYS),
                        "https://example.com/cqww"
                ),
                new ContestICalDto(
                        "ARRL 10m Contest",
                        now.plus(7, ChronoUnit.DAYS),
                        now.plus(9, ChronoUnit.DAYS),
                        "https://example.com/arrl10m"
                )
        );
    }

    private ContestEntity createTestEntity() {
        Instant now = Instant.now();
        return new ContestEntity(
                "Test Contest",
                now.plus(1, ChronoUnit.DAYS),
                now.plus(3, ChronoUnit.DAYS),
                Set.of(),
                Set.of(),
                "ARRL",
                "https://example.com",
                null
        );
    }
}
