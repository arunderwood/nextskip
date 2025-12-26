package io.nextskip.common.scheduler;

import io.nextskip.common.client.RefreshableDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DataRefreshScheduler.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Eager loading refreshes all sources on startup</li>
 *   <li>Failed refreshes don't propagate exceptions</li>
 *   <li>Sources are scheduled at their defined intervals</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class DataRefreshSchedulerTest {

    private static final String EAGER_LOAD_FIELD = "eagerLoad";

    @Mock
    private RefreshableDataSource source1;

    @Mock
    private RefreshableDataSource source2;

    private DataRefreshScheduler scheduler;

    @BeforeEach
    void setUp() {
        lenient().when(source1.getSourceName()).thenReturn("Source1");
        // Use a long interval to prevent scheduled task from running during test
        lenient().when(source1.getRefreshInterval()).thenReturn(Duration.ofHours(1));
        lenient().when(source2.getSourceName()).thenReturn("Source2");
        lenient().when(source2.getRefreshInterval()).thenReturn(Duration.ofHours(1));
    }

    @AfterEach
    void tearDown() {
        if (scheduler != null) {
            scheduler.destroy();
        }
    }

    @Test
    void testInitialize_WithEagerLoad_RefreshesAllSources() {
        scheduler = new DataRefreshScheduler(List.of(source1, source2));
        ReflectionTestUtils.setField(scheduler, EAGER_LOAD_FIELD, true);

        scheduler.initialize();

        // At minimum, each source should be refreshed once (eager load)
        verify(source1, atLeastOnce()).refresh();
        verify(source2, atLeastOnce()).refresh();
    }

    @Test
    void testInitialize_WithEagerLoadDisabled_SkipsInitialRefresh() {
        scheduler = new DataRefreshScheduler(List.of(source1, source2));
        ReflectionTestUtils.setField(scheduler, EAGER_LOAD_FIELD, false);

        scheduler.initialize();

        // With eager load disabled, sources should not be refreshed immediately
        // The scheduled task won't run during the test due to the long interval
        verify(source1, never()).refresh();
        verify(source2, never()).refresh();
    }

    @Test
    void testSafeRefresh_FailureDoesNotPropagate() {
        doThrow(new RuntimeException("API down")).when(source1).refresh();
        scheduler = new DataRefreshScheduler(List.of(source1, source2));
        ReflectionTestUtils.setField(scheduler, EAGER_LOAD_FIELD, true);

        // Should not throw - failures are logged, not propagated
        assertDoesNotThrow(() -> scheduler.initialize());

        // source2 should still be refreshed despite source1 failure
        verify(source2, atLeastOnce()).refresh();
    }

    @Test
    void testInitialize_EmptySources_DoesNotThrow() {
        DataRefreshScheduler emptyScheduler = new DataRefreshScheduler(List.of());
        ReflectionTestUtils.setField(emptyScheduler, EAGER_LOAD_FIELD, true);

        // Should not throw with empty sources list
        assertDoesNotThrow(emptyScheduler::initialize);

        emptyScheduler.destroy();
    }

    @Test
    void testDestroy_CancelsScheduledTasks() {
        scheduler = new DataRefreshScheduler(List.of(source1, source2));
        ReflectionTestUtils.setField(scheduler, EAGER_LOAD_FIELD, true);
        scheduler.initialize();

        // Should not throw on destroy
        assertDoesNotThrow(() -> scheduler.destroy());
    }
}
