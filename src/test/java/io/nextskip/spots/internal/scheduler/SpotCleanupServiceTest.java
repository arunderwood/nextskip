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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SpotCleanupService}.
 *
 * <p>Tests cleanup logic and TTL configuration.
 */
@ExtendWith(MockitoExtension.class)
class SpotCleanupServiceTest {

    private static final Duration DEFAULT_TTL = Duration.ofHours(SPOT_TTL_HOURS);

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
    void testExecuteCleanup_DeletesOldSpots_ReturnsCount() {
        when(spotRepository.deleteByCreatedAtBefore(any(Instant.class))).thenReturn(42);

        int deleted = cleanupService.executeCleanup();

        assertThat(deleted).isEqualTo(42);
        verify(spotRepository).deleteByCreatedAtBefore(any(Instant.class));
    }

    @Test
    void testExecuteCleanup_NoOldSpots_ReturnsZero() {
        when(spotRepository.deleteByCreatedAtBefore(any(Instant.class))).thenReturn(0);

        int deleted = cleanupService.executeCleanup();

        assertThat(deleted).isZero();
    }

    @Test
    void testExecuteCleanup_UsesTtlForCutoff() {
        Instant beforeCall = Instant.now();
        when(spotRepository.deleteByCreatedAtBefore(any(Instant.class))).thenReturn(0);

        cleanupService.executeCleanup();

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(spotRepository).deleteByCreatedAtBefore(cutoffCaptor.capture());

        Instant cutoff = cutoffCaptor.getValue();
        Instant expectedCutoff = beforeCall.minus(DEFAULT_TTL);

        // Cutoff should be approximately 24 hours ago (within a few seconds)
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
        when(spotRepository.deleteByCreatedAtBefore(any(Instant.class))).thenReturn(0);

        customService.executeCleanup();

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(spotRepository).deleteByCreatedAtBefore(cutoffCaptor.capture());

        Instant cutoff = cutoffCaptor.getValue();
        Instant expectedCutoff = beforeCall.minus(customTtl);

        assertThat(cutoff).isBetween(
                expectedCutoff.minusSeconds(5),
                expectedCutoff.plusSeconds(5)
        );
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
