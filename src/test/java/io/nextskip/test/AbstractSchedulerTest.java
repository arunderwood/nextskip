package io.nextskip.test;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for scheduler integration tests.
 *
 * <p>Uses the scheduler-test profile which enables db-scheduler
 * with a long polling interval (1 hour) to prevent actual task execution
 * during tests.
 *
 * <p>Provides cleanup of the scheduled_tasks table between tests to ensure
 * isolation when testing scheduler state or task persistence.
 *
 * <p>Usage:
 * <pre>{@code
 * class MySchedulerTest extends AbstractSchedulerTest {
 *
 *     @Autowired
 *     private Scheduler scheduler;
 *
 *     @Test
 *     void testSchedulerBehavior() {
 *         // scheduled_tasks table is cleaned before this runs
 *     }
 * }
 * }</pre>
 *
 * <p>Note: Most tests should use the default test profile (scheduler disabled).
 * Only extend this class when specifically testing scheduler functionality.
 */
@SpringBootTest
@ActiveProfiles("scheduler-test")
public abstract class AbstractSchedulerTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Cleans the scheduled_tasks table before each test.
     *
     * <p>This ensures test isolation when verifying scheduler state,
     * task execution history, or task registration.
     */
    @BeforeEach
    void cleanScheduledTasks() {
        jdbcTemplate.execute("DELETE FROM scheduled_tasks");
    }
}
