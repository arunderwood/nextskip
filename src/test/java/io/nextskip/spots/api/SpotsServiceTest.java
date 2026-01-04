package io.nextskip.spots.api;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.nextskip.spots.internal.SpotsServiceImpl;
import io.nextskip.spots.internal.client.SpotSource;
import io.nextskip.spots.internal.stream.SpotStreamProcessor;
import io.nextskip.spots.model.BandActivity;
import io.nextskip.spots.persistence.entity.SpotEntity;
import io.nextskip.spots.persistence.repository.SpotRepository;
import io.nextskip.test.fixtures.SpotFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SpotsService}.
 *
 * <p>Tests the public API for spot status and statistics.
 */
@ExtendWith(MockitoExtension.class)
class SpotsServiceTest {

    private static final Instant BASE_TIME = Instant.parse("2023-06-15T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(BASE_TIME, ZoneId.of("UTC"));

    @Mock
    private SpotSource spotSource;

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private SpotStreamProcessor streamProcessor;

    @Mock
    private LoadingCache<String, Map<String, BandActivity>> bandActivityCache;

    private SpotsService spotsService;

    @BeforeEach
    void setUp() {
        spotsService = new SpotsServiceImpl(spotSource, spotRepository, streamProcessor,
                bandActivityCache, FIXED_CLOCK);
    }

    // ===========================================
    // isConnected tests
    // ===========================================

    @Test
    void testIsConnected_SourceConnected_ReturnsTrue() {
        when(spotSource.isConnected()).thenReturn(true);

        boolean connected = spotsService.isConnected();

        assertThat(connected).isTrue();
        verify(spotSource).isConnected();
    }

    @Test
    void testIsConnected_SourceDisconnected_ReturnsFalse() {
        when(spotSource.isConnected()).thenReturn(false);

        boolean connected = spotsService.isConnected();

        assertThat(connected).isFalse();
    }

    // ===========================================
    // getSourceName tests
    // ===========================================

    @Test
    void testGetSourceName_ReturnsSourceName() {
        when(spotSource.getSourceName()).thenReturn("PSKReporter MQTT");

        String name = spotsService.getSourceName();

        assertThat(name).isEqualTo("PSKReporter MQTT");
        verify(spotSource).getSourceName();
    }

    // ===========================================
    // getSpotCount tests
    // ===========================================

    @Test
    void testGetSpotCount_EmptyRepository_ReturnsZero() {
        when(spotRepository.count()).thenReturn(0L);

        long count = spotsService.getSpotCount();

        assertThat(count).isZero();
        verify(spotRepository).count();
    }

    @Test
    void testGetSpotCount_WithSpots_ReturnsCount() {
        when(spotRepository.count()).thenReturn(12345L);

        long count = spotsService.getSpotCount();

        assertThat(count).isEqualTo(12345L);
    }

    // ===========================================
    // getLastSpotTime tests
    // ===========================================

    @Test
    void testGetLastSpotTime_NoSpots_ReturnsEmpty() {
        when(spotRepository.findTopByOrderBySpottedAtDesc()).thenReturn(Optional.empty());

        Optional<Instant> lastTime = spotsService.getLastSpotTime();

        assertThat(lastTime).isEmpty();
        verify(spotRepository).findTopByOrderBySpottedAtDesc();
    }

    @Test
    void testGetLastSpotTime_WithSpots_ReturnsMostRecentTime() {
        SpotEntity entity = SpotFixtures.spotEntity(
                SpotFixtures.spot().spottedAt(BASE_TIME).build(),
                FIXED_CLOCK
        );
        when(spotRepository.findTopByOrderBySpottedAtDesc()).thenReturn(Optional.of(entity));

        Optional<Instant> lastTime = spotsService.getLastSpotTime();

        assertThat(lastTime).isPresent();
        assertThat(lastTime.get()).isEqualTo(BASE_TIME);
    }

    // ===========================================
    // getSpotsProcessed tests
    // ===========================================

    @Test
    void testGetSpotsProcessed_ReturnsProcessorCount() {
        when(streamProcessor.getSpotsProcessed()).thenReturn(99999L);

        long processed = spotsService.getSpotsProcessed();

        assertThat(processed).isEqualTo(99999L);
        verify(streamProcessor).getSpotsProcessed();
    }

    @Test
    void testGetSpotsProcessed_NoProcessing_ReturnsZero() {
        when(streamProcessor.getSpotsProcessed()).thenReturn(0L);

        long processed = spotsService.getSpotsProcessed();

        assertThat(processed).isZero();
    }

    // ===========================================
    // getBatchesPersisted tests
    // ===========================================

    @Test
    void testGetBatchesPersisted_ReturnsProcessorCount() {
        when(streamProcessor.getBatchesPersisted()).thenReturn(500L);

        long batches = spotsService.getBatchesPersisted();

        assertThat(batches).isEqualTo(500L);
        verify(streamProcessor).getBatchesPersisted();
    }

    @Test
    void testGetBatchesPersisted_NoBatches_ReturnsZero() {
        when(streamProcessor.getBatchesPersisted()).thenReturn(0L);

        long batches = spotsService.getBatchesPersisted();

        assertThat(batches).isZero();
    }

    // ===========================================
    // getSpotCountSince tests
    // ===========================================

    @Test
    void testGetSpotCountSince_ReturnsRepositoryCount() {
        when(spotRepository.countByCreatedAtAfter(any(Instant.class))).thenReturn(42L);

        long count = spotsService.getSpotCountSince(5);

        assertThat(count).isEqualTo(42L);
        verify(spotRepository).countByCreatedAtAfter(any(Instant.class));
    }

    @Test
    void testGetSpotCountSince_NoRecentSpots_ReturnsZero() {
        when(spotRepository.countByCreatedAtAfter(any(Instant.class))).thenReturn(0L);

        long count = spotsService.getSpotCountSince(10);

        assertThat(count).isZero();
    }

    @Test
    void testGetSpotCountSince_ZeroMinutes_QueriesRecentSpots() {
        when(spotRepository.countByCreatedAtAfter(any(Instant.class))).thenReturn(100L);

        long count = spotsService.getSpotCountSince(0);

        assertThat(count).isEqualTo(100L);
    }

    // ===========================================
    // Constructor tests
    // ===========================================

    @Test
    void testConstructor_AllDependenciesProvided_CreatesService() {
        SpotsService service = new SpotsServiceImpl(spotSource, spotRepository, streamProcessor,
                bandActivityCache, FIXED_CLOCK);

        // Verify service can call methods without NullPointerException
        when(spotSource.isConnected()).thenReturn(true);
        assertThat(service.isConnected()).isTrue();
    }
}
