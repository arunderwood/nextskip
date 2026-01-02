package io.nextskip.common.scheduler;

import com.github.kagkarlsson.scheduler.Scheduler;
import io.nextskip.propagation.internal.scheduler.NoaaRefreshService;
import io.nextskip.propagation.persistence.repository.SolarIndicesRepository;
import io.nextskip.test.AbstractSchedulerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for task execution and database persistence.
 *
 * <p>These tests verify that:
 * <ul>
 *   <li>Refresh services persist data to the database when executed
 *   <li>Coordinators correctly detect empty vs populated repositories
 *   <li>All coordinators are properly configured with task references
 * </ul>
 *
 * <p>Note: These tests invoke refresh services directly rather than through
 * the scheduler to avoid test timing issues. The scheduler infrastructure
 * is tested separately in {@link DbSchedulerIntegrationTest}.
 *
 * <p>Tests use invariant-based assertions rather than exact value matching
 * since they call the real external API (following project testing guidelines).
 */
class TaskExecutionIntegrationTest extends AbstractSchedulerTest {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private SolarIndicesRepository solarIndicesRepository;

    @Autowired
    private List<RefreshTaskCoordinator> coordinators;

    @Autowired
    private NoaaRefreshService noaaRefreshService;

    @BeforeEach
    void cleanRepositories() {
        solarIndicesRepository.deleteAll();
    }

    @Test
    void testNeedsInitialLoad_EmptyRepository_ReturnsTrue() {
        // Find NOAA coordinator
        RefreshTaskCoordinator noaaCoordinator = findCoordinatorByName("NOAA");

        // With empty repository, needsInitialLoad should return true
        assertThat(noaaCoordinator.needsInitialLoad()).isTrue();
    }

    @Test
    void testNeedsInitialLoad_PopulatedRepository_ReturnsFalse() {
        // Find NOAA coordinator
        RefreshTaskCoordinator noaaCoordinator = findCoordinatorByName("NOAA");

        // Insert test data - simulate data from NOAA source
        // Constructor: solarFluxIndex, aIndex, kIndex, sunspotNumber, timestamp, source
        solarIndicesRepository.save(
                new io.nextskip.propagation.persistence.entity.SolarIndicesEntity(
                        150.0, 5, 3, 120, Instant.now(), "NOAA SWPC"));

        // With populated repository, needsInitialLoad should return false
        assertThat(noaaCoordinator.needsInitialLoad()).isFalse();
    }

    @Test
    void testRefreshService_NoaaRefresh_PersistsDataToDatabase() {
        // Verify repository is empty before execution
        assertThat(solarIndicesRepository.count()).isZero();

        // Execute refresh directly (this is what the scheduler task calls)
        noaaRefreshService.executeRefresh();

        // Verify data was persisted
        assertThat(solarIndicesRepository.count())
                .as("Solar indices should be persisted after refresh")
                .isGreaterThan(0);

        // Verify the persisted data came from NOAA
        // Note: We verify structure rather than exact values since the test
        // may use real API data if WireMock URL override isn't active
        var entities = solarIndicesRepository.findAll();
        assertThat(entities).isNotEmpty();

        var entity = entities.iterator().next();
        assertThat(entity.getSolarFluxIndex())
                .as("Solar flux index should be positive")
                .isGreaterThan(0);
        assertThat(entity.getSunspotNumber())
                .as("Sunspot number should be non-negative")
                .isGreaterThanOrEqualTo(0);
        assertThat(entity.getSource()).isEqualTo("NOAA SWPC");
    }

    @Test
    void testCoordinators_DiscoveredViaSpring() {
        // Verify all 7 coordinators are present
        assertThat(coordinators).hasSize(7);

        // Verify each has required methods returning non-null
        for (RefreshTaskCoordinator coordinator : coordinators) {
            assertThat(coordinator.getTaskName())
                    .as("Task name for coordinator should not be blank")
                    .isNotBlank();
            assertThat(coordinator.getRecurringTask())
                    .as("RecurringTask for %s should not be null", coordinator.getTaskName())
                    .isNotNull();
        }
    }

    @Test
    void testAllCoordinators_HaveNeedsInitialLoadMethod() {
        // All coordinators should implement needsInitialLoad without throwing
        for (RefreshTaskCoordinator coordinator : coordinators) {
            // This should not throw
            boolean needsLoad = coordinator.needsInitialLoad();
            // Result is boolean, so either true or false is valid
            assertThat(needsLoad).isIn(true, false);
        }
    }

    private RefreshTaskCoordinator findCoordinatorByName(String name) {
        return coordinators.stream()
                .filter(c -> c.getTaskName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Coordinator not found: " + name));
    }
}
