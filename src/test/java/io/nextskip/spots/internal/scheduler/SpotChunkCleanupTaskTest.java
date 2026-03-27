package io.nextskip.spots.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import io.nextskip.spots.persistence.repository.SpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link SpotChunkCleanupTask}.
 *
 * <p>Tests the recurring task configuration for dropping old
 * hypertable chunks from the spots table.
 */
@ExtendWith(MockitoExtension.class)
class SpotChunkCleanupTaskTest {

    private static final String EXPECTED_TASK_NAME = "spot-chunk-cleanup";
    private static final String EXPECTED_RETENTION_INTERVAL = "6 hours";

    @Mock
    private SpotRepository spotRepository;

    private SpotChunkCleanupTask task;

    @BeforeEach
    void setUp() {
        task = new SpotChunkCleanupTask();
    }

    @Nested
    class TaskConfigurationTests {

        @Test
        void testSpotChunkCleanupRecurringTask_Created_HasCorrectName() {
            // When
            RecurringTask<Void> recurringTask = task.spotChunkCleanupRecurringTask(spotRepository);

            // Then
            assertThat(recurringTask.getName()).isEqualTo(EXPECTED_TASK_NAME);
        }

        @Test
        void testSpotChunkCleanupRecurringTask_Created_IsNotNull() {
            // When
            RecurringTask<Void> recurringTask = task.spotChunkCleanupRecurringTask(spotRepository);

            // Then
            assertThat(recurringTask)
                    .as("Recurring task bean should be created successfully")
                    .isNotNull();
        }
    }

    @Nested
    class TaskExecutionTests {

        @SuppressWarnings("unchecked")
        @Test
        void testExecute_CallsDropOldChunks_WithRetentionInterval() throws Exception {
            // Given
            RecurringTask<Void> recurringTask = task.spotChunkCleanupRecurringTask(spotRepository);
            TaskInstance<Void> taskInstance = mock(TaskInstance.class);
            ExecutionContext executionContext = mock(ExecutionContext.class);

            // When
            recurringTask.execute(taskInstance, executionContext);

            // Then
            verify(spotRepository).dropOldChunks(EXPECTED_RETENTION_INTERVAL);
        }
    }
}
