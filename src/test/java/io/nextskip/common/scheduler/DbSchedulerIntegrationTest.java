package io.nextskip.common.scheduler;

import com.github.kagkarlsson.scheduler.Scheduler;
import io.nextskip.test.AbstractSchedulerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for db-scheduler configuration and task registration.
 *
 * <p>These tests verify that the db-scheduler components are properly configured
 * and that the application context starts successfully with the scheduling infrastructure.
 *
 * <p>With the manual DbSchedulerConfig (Spring Boot 4 workaround), the Scheduler bean
 * should now be created successfully in all environments including tests.
 *
 * <p>Extends AbstractSchedulerTest which provides scheduled_tasks table cleanup
 * and uses the scheduler-test profile.
 */
class DbSchedulerIntegrationTest extends AbstractSchedulerTest {

    // HamQSL solar+band consolidated into one coordinator
    private static final int EXPECTED_COORDINATOR_COUNT = 6;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private DataRefreshStartupHandler startupHandler;

    @Autowired
    private List<RefreshTaskCoordinator> coordinators;

    @Test
    void testApplicationContext_LoadsSuccessfully() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void testScheduler_BeanIsCreated() {
        assertThat(scheduler).isNotNull();
    }

    @Test
    void testScheduler_IsStartedAfterContextRefresh() {
        assertThat(scheduler.getSchedulerState().isStarted()).isTrue();
    }

    @Test
    void testDataRefreshStartupHandler_IsCreated() {
        // Now that Scheduler bean exists via DbSchedulerConfig,
        // the handler should be created (@ConditionalOnProperty)
        assertThat(startupHandler).isNotNull();
    }

    @Test
    void testRefreshTaskCoordinators_AllRegistered() {
        // Verify all 6 coordinators are discovered via Spring's component scanning
        assertThat(coordinators)
                .hasSize(EXPECTED_COORDINATOR_COUNT)
                .allSatisfy(coordinator -> {
                    assertThat(coordinator.getTaskName()).isNotBlank();
                    assertThat(coordinator.getRecurringTask()).isNotNull();
                });
    }

    @Test
    void testRefreshTaskCoordinators_HaveExpectedNames() {
        // Verify we have all expected task types registered (by task ID)
        List<String> taskNames = coordinators.stream()
                .map(RefreshTaskCoordinator::getTaskName)
                .toList();

        // HamQSL solar+band consolidated into single hamqsl-refresh task
        assertThat(taskNames).contains(
                "pota-refresh", "sota-refresh", "noaa-refresh",
                "hamqsl-refresh", "contest-refresh", "meteor-refresh");
    }

    @Test
    void testRefreshTaskCoordinators_HaveExpectedDisplayNames() {
        // Verify display names are user-friendly (same as task names after consolidation)
        List<String> displayNames = coordinators.stream()
                .map(RefreshTaskCoordinator::getDisplayName)
                .toList();

        assertThat(displayNames)
                .contains("POTA", "SOTA", "NOAA", "HamQSL", "Contest", "Meteor");
    }

    @Test
    void testRecurringTaskBeans_RegisteredForScheduler() {
        // Verify that RecurringTask beans are registered
        // These are the beans that db-scheduler will pick up when it starts
        assertThat(applicationContext.containsBean("potaRecurringTask")).isTrue();
        assertThat(applicationContext.containsBean("sotaRecurringTask")).isTrue();
        assertThat(applicationContext.containsBean("noaaRecurringTask")).isTrue();
        // HamQSL solar+band consolidated into single hamQslRecurringTask
        assertThat(applicationContext.containsBean("hamQslRecurringTask")).isTrue();
        assertThat(applicationContext.containsBean("contestRecurringTask")).isTrue();
        assertThat(applicationContext.containsBean("meteorRecurringTask")).isTrue();
    }
}
