package io.nextskip.common.scheduler;

import com.github.kagkarlsson.scheduler.Scheduler;
import io.nextskip.activations.internal.scheduler.PotaRefreshTask;
import io.nextskip.activations.internal.scheduler.SotaRefreshTask;
import io.nextskip.contests.internal.scheduler.ContestRefreshTask;
import io.nextskip.meteors.internal.scheduler.MeteorRefreshTask;
import io.nextskip.propagation.internal.scheduler.HamQslBandRefreshTask;
import io.nextskip.propagation.internal.scheduler.HamQslSolarRefreshTask;
import io.nextskip.propagation.internal.scheduler.NoaaRefreshTask;
import io.nextskip.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for db-scheduler configuration and task registration.
 *
 * <p>These tests verify that the db-scheduler components are properly configured
 * and that the application context starts successfully with the scheduling infrastructure.
 *
 * <p>With the manual DbSchedulerConfig (Spring Boot 4 workaround), the Scheduler bean
 * should now be created successfully in all environments including tests.
 */
@SpringBootTest
class DbSchedulerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private DataRefreshStartupHandler startupHandler;

    @Autowired
    private PotaRefreshTask potaRefreshTask;

    @Autowired
    private SotaRefreshTask sotaRefreshTask;

    @Autowired
    private NoaaRefreshTask noaaRefreshTask;

    @Autowired
    private HamQslSolarRefreshTask hamQslSolarRefreshTask;

    @Autowired
    private HamQslBandRefreshTask hamQslBandRefreshTask;

    @Autowired
    private ContestRefreshTask contestRefreshTask;

    @Autowired
    private MeteorRefreshTask meteorRefreshTask;

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
        // the handler should be created (@ConditionalOnBean(Scheduler.class))
        assertThat(startupHandler).isNotNull();
    }

    @Test
    void testRefreshTaskConfigurations_AllPresent() {
        // Verify all task configuration beans are loaded
        assertThat(potaRefreshTask).isNotNull();
        assertThat(sotaRefreshTask).isNotNull();
        assertThat(noaaRefreshTask).isNotNull();
        assertThat(hamQslSolarRefreshTask).isNotNull();
        assertThat(hamQslBandRefreshTask).isNotNull();
        assertThat(contestRefreshTask).isNotNull();
        assertThat(meteorRefreshTask).isNotNull();
    }

    @Test
    void testRecurringTaskBeans_RegisteredForScheduler() {
        // Verify that RecurringTask beans are registered
        // These are the beans that db-scheduler will pick up when it starts
        assertThat(applicationContext.containsBean("potaRecurringTask")).isTrue();
        assertThat(applicationContext.containsBean("sotaRecurringTask")).isTrue();
        assertThat(applicationContext.containsBean("noaaRecurringTask")).isTrue();
        assertThat(applicationContext.containsBean("hamQslSolarRecurringTask")).isTrue();
        assertThat(applicationContext.containsBean("hamQslBandRecurringTask")).isTrue();
        assertThat(applicationContext.containsBean("contestRecurringTask")).isTrue();
        assertThat(applicationContext.containsBean("meteorRecurringTask")).isTrue();
    }
}
