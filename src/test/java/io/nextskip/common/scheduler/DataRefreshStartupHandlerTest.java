package io.nextskip.common.scheduler;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import io.nextskip.activations.internal.scheduler.PotaRefreshTask;
import io.nextskip.activations.internal.scheduler.SotaRefreshTask;
import io.nextskip.activations.persistence.repository.ActivationRepository;
import io.nextskip.contests.internal.scheduler.ContestRefreshTask;
import io.nextskip.contests.persistence.repository.ContestRepository;
import io.nextskip.meteors.internal.scheduler.MeteorRefreshTask;
import io.nextskip.meteors.persistence.repository.MeteorShowerRepository;
import io.nextskip.propagation.internal.scheduler.HamQslBandRefreshTask;
import io.nextskip.propagation.internal.scheduler.HamQslSolarRefreshTask;
import io.nextskip.propagation.internal.scheduler.NoaaRefreshTask;
import io.nextskip.propagation.persistence.repository.BandConditionRepository;
import io.nextskip.propagation.persistence.repository.SolarIndicesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DataRefreshStartupHandler.
 */
@ExtendWith(MockitoExtension.class)
class DataRefreshStartupHandlerTest {

    @Mock
    private Scheduler scheduler;

    @Mock
    private PotaRefreshTask potaRefreshTask;

    @Mock
    private SotaRefreshTask sotaRefreshTask;

    @Mock
    private NoaaRefreshTask noaaRefreshTask;

    @Mock
    private HamQslSolarRefreshTask hamQslSolarRefreshTask;

    @Mock
    private HamQslBandRefreshTask hamQslBandRefreshTask;

    @Mock
    private ContestRefreshTask contestRefreshTask;

    @Mock
    private MeteorRefreshTask meteorRefreshTask;

    @Mock
    private ActivationRepository activationRepository;

    @Mock
    private SolarIndicesRepository solarIndicesRepository;

    @Mock
    private BandConditionRepository bandConditionRepository;

    @Mock
    private ContestRepository contestRepository;

    @Mock
    private MeteorShowerRepository meteorShowerRepository;

    @Mock
    private RecurringTask<Void> potaTask;

    @Mock
    private RecurringTask<Void> sotaTask;

    @Mock
    private RecurringTask<Void> noaaTask;

    @Mock
    private RecurringTask<Void> hamQslSolarTask;

    @Mock
    private RecurringTask<Void> hamQslBandTask;

    @Mock
    private RecurringTask<Void> contestTask;

    @Mock
    private RecurringTask<Void> meteorTask;

    @Mock
    private ApplicationReadyEvent applicationReadyEvent;

    @Mock
    private TaskInstance<Void> taskInstance;

    @Test
    void testOnApplicationReady_EagerLoadDisabled_SkipsAllChecks() {
        DataRefreshStartupHandler handler = createHandler(false);

        handler.onApplicationReady(applicationReadyEvent);

        verify(potaRefreshTask, never()).needsInitialLoad(any());
        verify(scheduler, never()).reschedule(any(), any(Instant.class));
    }

    @Test
    void testOnApplicationReady_AllRepositoriesHaveData_NoReschedules() {
        DataRefreshStartupHandler handler = createHandler(true);
        setupAllRepositoriesWithData();

        handler.onApplicationReady(applicationReadyEvent);

        verify(scheduler, never()).reschedule(any(), any(Instant.class));
    }

    @Test
    void testOnApplicationReady_PotaNeedsLoad_ReschedulesPota() {
        DataRefreshStartupHandler handler = createHandler(true);
        setupAllRepositoriesWithData();
        when(potaRefreshTask.needsInitialLoad(activationRepository)).thenReturn(true);
        when(potaTask.instance(RecurringTask.INSTANCE)).thenReturn(taskInstance);

        handler.onApplicationReady(applicationReadyEvent);

        verify(scheduler).reschedule(any(), any(Instant.class));
    }

    @Test
    void testOnApplicationReady_SotaNeedsLoad_ReschedulesSota() {
        DataRefreshStartupHandler handler = createHandler(true);
        setupAllRepositoriesWithData();
        when(sotaRefreshTask.needsInitialLoad(activationRepository)).thenReturn(true);
        when(sotaTask.instance(RecurringTask.INSTANCE)).thenReturn(taskInstance);

        handler.onApplicationReady(applicationReadyEvent);

        verify(scheduler).reschedule(any(), any(Instant.class));
    }

    @Test
    void testOnApplicationReady_NoaaNeedsLoad_ReschedulesNoaa() {
        DataRefreshStartupHandler handler = createHandler(true);
        setupAllRepositoriesWithData();
        when(noaaRefreshTask.needsInitialLoad(solarIndicesRepository)).thenReturn(true);
        when(noaaTask.instance(RecurringTask.INSTANCE)).thenReturn(taskInstance);

        handler.onApplicationReady(applicationReadyEvent);

        verify(scheduler).reschedule(any(), any(Instant.class));
    }

    @Test
    void testOnApplicationReady_HamQslSolarNeedsLoad_ReschedulesHamQslSolar() {
        DataRefreshStartupHandler handler = createHandler(true);
        setupAllRepositoriesWithData();
        when(hamQslSolarRefreshTask.needsInitialLoad(solarIndicesRepository)).thenReturn(true);
        when(hamQslSolarTask.instance(RecurringTask.INSTANCE)).thenReturn(taskInstance);

        handler.onApplicationReady(applicationReadyEvent);

        verify(scheduler).reschedule(any(), any(Instant.class));
    }

    @Test
    void testOnApplicationReady_HamQslBandNeedsLoad_ReschedulesHamQslBand() {
        DataRefreshStartupHandler handler = createHandler(true);
        setupAllRepositoriesWithData();
        when(hamQslBandRefreshTask.needsInitialLoad(bandConditionRepository)).thenReturn(true);
        when(hamQslBandTask.instance(RecurringTask.INSTANCE)).thenReturn(taskInstance);

        handler.onApplicationReady(applicationReadyEvent);

        verify(scheduler).reschedule(any(), any(Instant.class));
    }

    @Test
    void testOnApplicationReady_ContestNeedsLoad_ReschedulesContest() {
        DataRefreshStartupHandler handler = createHandler(true);
        setupAllRepositoriesWithData();
        when(contestRefreshTask.needsInitialLoad(contestRepository)).thenReturn(true);
        when(contestTask.instance(RecurringTask.INSTANCE)).thenReturn(taskInstance);

        handler.onApplicationReady(applicationReadyEvent);

        verify(scheduler).reschedule(any(), any(Instant.class));
    }

    @Test
    void testOnApplicationReady_MeteorNeedsLoad_ReschedulesMeteor() {
        DataRefreshStartupHandler handler = createHandler(true);
        setupAllRepositoriesWithData();
        when(meteorRefreshTask.needsInitialLoad(meteorShowerRepository)).thenReturn(true);
        when(meteorTask.instance(RecurringTask.INSTANCE)).thenReturn(taskInstance);

        handler.onApplicationReady(applicationReadyEvent);

        verify(scheduler).reschedule(any(), any(Instant.class));
    }

    @Test
    void testOnApplicationReady_RescheduleThrowsException_ContinuesProcessing() {
        DataRefreshStartupHandler handler = createHandler(true);
        setupAllRepositoriesWithData();
        when(potaRefreshTask.needsInitialLoad(activationRepository)).thenReturn(true);
        when(potaTask.instance(RecurringTask.INSTANCE)).thenReturn(taskInstance);
        when(scheduler.reschedule(any(), any(Instant.class)))
                .thenThrow(new RuntimeException("Scheduler error"));

        // Should not throw - exception is caught and logged
        handler.onApplicationReady(applicationReadyEvent);

        verify(scheduler).reschedule(any(), any(Instant.class));
    }

    @Test
    void testOnApplicationReady_MultipleRepositoriesNeedLoad_ReschedulesMultiple() {
        DataRefreshStartupHandler handler = createHandler(true);
        setupAllRepositoriesWithData();
        when(potaRefreshTask.needsInitialLoad(activationRepository)).thenReturn(true);
        when(noaaRefreshTask.needsInitialLoad(solarIndicesRepository)).thenReturn(true);
        when(potaTask.instance(RecurringTask.INSTANCE)).thenReturn(taskInstance);
        when(noaaTask.instance(RecurringTask.INSTANCE)).thenReturn(taskInstance);

        handler.onApplicationReady(applicationReadyEvent);

        // Verify reschedule was called twice (once for POTA, once for NOAA)
        verify(scheduler, times(2)).reschedule(any(), any(Instant.class));
    }

    private DataRefreshStartupHandler createHandler(boolean eagerLoadEnabled) {
        return new DataRefreshStartupHandler(
                scheduler,
                eagerLoadEnabled,
                potaRefreshTask,
                sotaRefreshTask,
                noaaRefreshTask,
                hamQslSolarRefreshTask,
                hamQslBandRefreshTask,
                contestRefreshTask,
                meteorRefreshTask,
                activationRepository,
                solarIndicesRepository,
                bandConditionRepository,
                contestRepository,
                meteorShowerRepository,
                potaTask,
                sotaTask,
                noaaTask,
                hamQslSolarTask,
                hamQslBandTask,
                contestTask,
                meteorTask
        );
    }

    private void setupAllRepositoriesWithData() {
        when(potaRefreshTask.needsInitialLoad(activationRepository)).thenReturn(false);
        when(sotaRefreshTask.needsInitialLoad(activationRepository)).thenReturn(false);
        when(noaaRefreshTask.needsInitialLoad(solarIndicesRepository)).thenReturn(false);
        when(hamQslSolarRefreshTask.needsInitialLoad(solarIndicesRepository)).thenReturn(false);
        when(hamQslBandRefreshTask.needsInitialLoad(bandConditionRepository)).thenReturn(false);
        when(contestRefreshTask.needsInitialLoad(contestRepository)).thenReturn(false);
        when(meteorRefreshTask.needsInitialLoad(meteorShowerRepository)).thenReturn(false);
    }
}
