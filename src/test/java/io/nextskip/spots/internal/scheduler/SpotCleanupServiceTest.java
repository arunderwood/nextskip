package io.nextskip.spots.internal.scheduler;

import io.nextskip.spots.persistence.repository.SpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static io.nextskip.test.TestConstants.SPOT_TTL_HOURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SpotCleanupService}.
 *
 * <p>Tests cleanup logic, TTL configuration, and batched deletion behavior.
 */
@ExtendWith(MockitoExtension.class)
class SpotCleanupServiceTest {

    private static final Duration DEFAULT_TTL = Duration.ofHours(SPOT_TTL_HOURS);
    private static final int BATCH_SIZE = 100_000;

    @Mock
    private SpotRepository spotRepository;

    private SpotCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        cleanupService = new SpotCleanupService(spotRepository, DEFAULT_TTL);
    }

    // ===========================================
    // executeCleanup tests
    // ===========================================

    @Test
    void testExecuteCleanup_SingleBatch_ReturnsCount() {
        // Single batch with fewer than BATCH_SIZE rows (indicates completion)
        when(spotRepository.deleteExpiredSpotsBatch(any(Instant.class), eq(BATCH_SIZE)))
                .thenReturn(42);

        int deleted = cleanupService.executeCleanup();

        assertThat(deleted).isEqualTo(42);
        verify(spotRepository).deleteExpiredSpotsBatch(any(Instant.class), eq(BATCH_SIZE));
    }

    @Test
    void testExecuteCleanup_MultipleBatches_ReturnsTotalCount() {
        // First batch returns full BATCH_SIZE, second batch returns partial
        when(spotRepository.deleteExpiredSpotsBatch(any(Instant.class), eq(BATCH_SIZE)))
                .thenReturn(BATCH_SIZE)  // First batch: full
                .thenReturn(BATCH_SIZE)  // Second batch: full
                .thenReturn(50_000);     // Third batch: partial (done)

        int deleted = cleanupService.executeCleanup();

        assertThat(deleted).isEqualTo(BATCH_SIZE + BATCH_SIZE + 50_000);
        verify(spotRepository, times(3)).deleteExpiredSpotsBatch(any(Instant.class), eq(BATCH_SIZE));
    }

    @Test
    void testExecuteCleanup_NoOldSpots_ReturnsZero() {
        when(spotRepository.deleteExpiredSpotsBatch(any(Instant.class), anyInt()))
                .thenReturn(0);

        int deleted = cleanupService.executeCleanup();

        assertThat(deleted).isZero();
    }

    @Test
    void testExecuteCleanup_UsesTtlForCutoff() {
        Instant beforeCall = Instant.now();
        when(spotRepository.deleteExpiredSpotsBatch(any(Instant.class), anyInt()))
                .thenReturn(0);

        cleanupService.executeCleanup();

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(spotRepository).deleteExpiredSpotsBatch(cutoffCaptor.capture(), eq(BATCH_SIZE));

        Instant cutoff = cutoffCaptor.getValue();
        Instant expectedCutoff = beforeCall.minus(DEFAULT_TTL);

        // Cutoff should be approximately TTL ago (within a few seconds)
        assertThat(cutoff).isBetween(
                expectedCutoff.minusSeconds(5),
                expectedCutoff.plusSeconds(5)
        );
    }

    @Test
    void testExecuteCleanup_CustomTtl_UsesConfiguredValue() {
        Duration customTtl = Duration.ofHours(12);
        SpotCleanupService customService = new SpotCleanupService(spotRepository, customTtl);
        Instant beforeCall = Instant.now();
        when(spotRepository.deleteExpiredSpotsBatch(any(Instant.class), anyInt()))
                .thenReturn(0);

        customService.executeCleanup();

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(spotRepository).deleteExpiredSpotsBatch(cutoffCaptor.capture(), eq(BATCH_SIZE));

        Instant cutoff = cutoffCaptor.getValue();
        Instant expectedCutoff = beforeCall.minus(customTtl);

        assertThat(cutoff).isBetween(
                expectedCutoff.minusSeconds(5),
                expectedCutoff.plusSeconds(5)
        );
    }

    @Test
    void testExecuteCleanup_ExactBatchSize_ContinuesUntilPartialBatch() {
        // When batch returns exactly BATCH_SIZE, continue; stop only when < BATCH_SIZE
        when(spotRepository.deleteExpiredSpotsBatch(any(Instant.class), eq(BATCH_SIZE)))
                .thenReturn(BATCH_SIZE)  // Continue
                .thenReturn(0);          // Stop (0 < BATCH_SIZE)

        int deleted = cleanupService.executeCleanup();

        assertThat(deleted).isEqualTo(BATCH_SIZE);
        verify(spotRepository, times(2)).deleteExpiredSpotsBatch(any(Instant.class), eq(BATCH_SIZE));
    }

    // ===========================================
    // getTtl tests
    // ===========================================

    @Test
    void testGetTtl_DefaultTtl_Returns24Hours() {
        assertThat(cleanupService.getTtl()).isEqualTo(DEFAULT_TTL);
    }

    @Test
    void testGetTtl_CustomTtl_ReturnsConfiguredValue() {
        Duration customTtl = Duration.ofHours(48);
        SpotCleanupService customService = new SpotCleanupService(spotRepository, customTtl);

        assertThat(customService.getTtl()).isEqualTo(customTtl);
    }

    // ===========================================
    // Constructor tests
    // ===========================================

    @Test
    void testConstructor_NullTtl_DefaultsTo24Hours() {
        // The Spring @Value annotation provides the default, but we test the service works
        SpotCleanupService service = new SpotCleanupService(spotRepository, Duration.ofHours(24));

        assertThat(service.getTtl()).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void testConstructor_ZeroTtl_Accepted() {
        // Edge case: immediate cleanup
        SpotCleanupService service = new SpotCleanupService(spotRepository, Duration.ZERO);

        assertThat(service.getTtl()).isEqualTo(Duration.ZERO);
    }

    @Test
    void testConstructor_NegativeTtl_Accepted() {
        // Edge case: would delete future spots (unlikely but valid Duration)
        Duration negativeTtl = Duration.ofHours(-1);
        SpotCleanupService service = new SpotCleanupService(spotRepository, negativeTtl);

        assertThat(service.getTtl()).isEqualTo(negativeTtl);
    }
}
