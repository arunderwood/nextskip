package io.nextskip.spots.internal.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import io.nextskip.spots.persistence.repository.SpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BandActivityRefreshTask}.
 *
 * <p>Tests the task coordinator for band activity aggregation scheduling.
 */
@ExtendWith(MockitoExtension.class)
class BandActivityRefreshTaskTest {

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private BandActivityRefreshService refreshService;

    @Mock
    private RecurringTask<Void> mockRecurringTask;

    private BandActivityRefreshTask task;

    @BeforeEach
    void setUp() {
        task = new BandActivityRefreshTask(spotRepository);
    }

    // =========================================================================
    // Task Name Tests
    // =========================================================================

    @Nested
    class TaskNameTests {

        @Test
        void testGetTaskName_ReturnsBandActivity() {
            assertThat(task.getTaskName()).isEqualTo("Band Activity");
        }
    }

    // =========================================================================
    // Needs Initial Load Tests
    // =========================================================================

    @Nested
    class NeedsInitialLoadTests {

        @Test
        void testNeedsInitialLoad_RecentSpots_ReturnsTrue() {
            // Given: spots exist in the last 5 minutes
            when(spotRepository.countByCreatedAtAfter(any(Instant.class))).thenReturn(10L);

            // When
            boolean result = task.needsInitialLoad();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void testNeedsInitialLoad_NoRecentSpots_ReturnsFalse() {
            // Given: no spots in the last 5 minutes
            when(spotRepository.countByCreatedAtAfter(any(Instant.class))).thenReturn(0L);

            // When
            boolean result = task.needsInitialLoad();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        void testNeedsInitialLoad_ExactlyZeroSpots_ReturnsFalse() {
            // Given: exactly zero spots
            when(spotRepository.countByCreatedAtAfter(any(Instant.class))).thenReturn(0L);

            // When
            boolean result = task.needsInitialLoad();

            // Then
            assertThat(result)
                    .as("Should return false when count is 0")
                    .isFalse();
        }

        @Test
        void testNeedsInitialLoad_OneSpot_ReturnsTrue() {
            // Given: at least one spot exists
            when(spotRepository.countByCreatedAtAfter(any(Instant.class))).thenReturn(1L);

            // When
            boolean result = task.needsInitialLoad();

            // Then
            assertThat(result)
                    .as("Should return true when at least one spot exists")
                    .isTrue();
        }
    }

    // =========================================================================
    // Recurring Task Tests
    // =========================================================================

    @Nested
    class RecurringTaskTests {

        @Test
        void testGetRecurringTask_BeforeInjection_ReturnsNull() {
            // Before the task is injected, it should be null
            assertThat(task.getRecurringTask()).isNull();
        }

        @Test
        void testSetRecurringTask_InjectedTask_CanBeRetrieved() {
            // Given
            task.setRecurringTask(mockRecurringTask);

            // When
            RecurringTask<Void> result = task.getRecurringTask();

            // Then
            assertThat(result).isSameAs(mockRecurringTask);
        }

        @Test
        void testBandActivityRecurringTask_Created_HasCorrectName() {
            // Given the task is created by the bean method
            RecurringTask<Void> createdTask = task.bandActivityRecurringTask(refreshService);

            // Then
            assertThat(createdTask).isNotNull();
            assertThat(createdTask.getName()).isEqualTo("band-activity-refresh");
        }
    }
}
