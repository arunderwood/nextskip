package io.nextskip.common.scheduler;

import io.nextskip.common.client.RefreshableDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DataRefreshScheduler.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Scheduler bean is loaded in Spring context</li>
 *   <li>All RefreshableDataSource implementations are discovered</li>
 *   <li>OCP: sources are auto-discovered without scheduler modification</li>
 * </ul>
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "nextskip.refresh.eager-load=false"  // Disable eager loading for test stability
})
class DataRefreshSchedulerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("nextskip_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private DataRefreshScheduler scheduler;

    @Autowired
    private List<RefreshableDataSource> sources;

    @Test
    void testContext_SchedulerBeanLoaded() {
        assertNotNull(scheduler, "DataRefreshScheduler should be loaded as a Spring bean");
    }

    @Test
    void testContext_AllSourcesDiscovered() {
        // Should auto-discover all RefreshableDataSource implementations
        assertFalse(sources.isEmpty(), "At least one RefreshableDataSource should be discovered");
        assertTrue(sources.size() >= 6,
                "Expected at least 6 sources (POTA, SOTA, NOAA, HamQSL, Contests, Meteors), found: "
                        + sources.size());
    }

    @Test
    void testOcp_SourcesHaveRequiredMethods() {
        // Verify all sources implement the interface correctly
        for (RefreshableDataSource source : sources) {
            assertNotNull(source.getSourceName(), "Source name should not be null");
            assertFalse(source.getSourceName().isBlank(), "Source name should not be blank");
            assertNotNull(source.getRefreshInterval(), "Refresh interval should not be null");
            assertTrue(source.getRefreshInterval().toMillis() > 0,
                    "Refresh interval should be positive");
        }
    }

    @Test
    void testOcp_KnownSourcesDiscovered() {
        // Verify that the list contains expected source names
        List<String> sourceNames = sources.stream()
                .map(RefreshableDataSource::getSourceName)
                .toList();

        assertTrue(sourceNames.contains("POTA API"), "POTA source should be discovered");
        assertTrue(sourceNames.contains("SOTA API"), "SOTA source should be discovered");
        assertTrue(sourceNames.contains("NOAA"), "NOAA source should be discovered");
        assertTrue(sourceNames.contains("HamQSL"), "HamQSL source should be discovered");
        assertTrue(sourceNames.contains("WA7BNM Contest Calendar"), "WA7BNM (Contests) source should be discovered");
        assertTrue(sourceNames.contains("Meteor Showers"), "Meteor Showers source should be discovered");
    }
}
