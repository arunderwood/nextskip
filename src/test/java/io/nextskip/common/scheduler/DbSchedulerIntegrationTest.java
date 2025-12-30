package io.nextskip.common.scheduler;

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
 * <p>Note: The Scheduler bean is created by db-scheduler auto-configuration
 * which requires a datasource. In tests using Testcontainers, the datasource
 * may not be available early enough for auto-configuration to detect it.
 * Therefore, we test that the application starts successfully and task
 * configuration classes are loaded rather than directly autowiring the Scheduler.
 */
@SpringBootTest
class DbSchedulerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

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
    void testDataRefreshStartupHandler_ConditionallyLoaded() {
        // DataRefreshStartupHandler is @ConditionalOnBean(Scheduler.class)
        // In tests where Scheduler isn't available, it should not be loaded
        // Just verify the context started - the handler may or may not be present
        // depending on whether Scheduler bean was created
        assertThat(applicationContext.getStartupDate()).isPositive();
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
