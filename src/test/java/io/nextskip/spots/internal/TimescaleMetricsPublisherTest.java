package io.nextskip.spots.internal;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TimescaleMetricsPublisher}.
 *
 * <p>Tests gauge registration and metric refresh behavior using
 * a mock JdbcTemplate and SimpleMeterRegistry.
 */
@ExtendWith(MockitoExtension.class)
class TimescaleMetricsPublisherTest {

    private static final String TOTAL_CHUNKS_METRIC = "nextskip.timescaledb.chunks.total";
    private static final String COMPRESSED_CHUNKS_METRIC = "nextskip.timescaledb.chunks.compressed";

    @Mock
    private JdbcTemplate jdbcTemplate;

    private SimpleMeterRegistry registry;
    private TimescaleMetricsPublisher publisher;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        publisher = new TimescaleMetricsPublisher(jdbcTemplate, registry);
    }

    // ===========================================
    // Constructor / gauge registration tests
    // ===========================================

    @Test
    void testConstructor_RegistersTotalChunksGauge() {
        Gauge gauge = registry.find(TOTAL_CHUNKS_METRIC).gauge();

        assertThat(gauge).isNotNull();
        assertThat(gauge.getId().getTag("hypertable")).isEqualTo("spots");
    }

    @Test
    void testConstructor_RegistersCompressedChunksGauge() {
        Gauge gauge = registry.find(COMPRESSED_CHUNKS_METRIC).gauge();

        assertThat(gauge).isNotNull();
        assertThat(gauge.getId().getTag("hypertable")).isEqualTo("spots");
    }

    @Test
    void testConstructor_GaugesStartAtZero() {
        assertThat(registry.find(TOTAL_CHUNKS_METRIC).gauge().value()).isZero();
        assertThat(registry.find(COMPRESSED_CHUNKS_METRIC).gauge().value()).isZero();
    }

    // ===========================================
    // refreshMetrics success tests
    // ===========================================

    @Test
    void testRefreshMetrics_QueriesReturnValues_UpdatesGauges() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(12L)
                .thenReturn(8L);

        publisher.refreshMetrics();

        assertThat(registry.find(TOTAL_CHUNKS_METRIC).gauge().value()).isEqualTo(12.0);
        assertThat(registry.find(COMPRESSED_CHUNKS_METRIC).gauge().value()).isEqualTo(8.0);
    }

    @Test
    void testRefreshMetrics_CalledTwice_UpdatesToNewValues() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(5L)
                .thenReturn(3L)
                .thenReturn(10L)
                .thenReturn(7L);

        publisher.refreshMetrics();
        publisher.refreshMetrics();

        assertThat(registry.find(TOTAL_CHUNKS_METRIC).gauge().value()).isEqualTo(10.0);
        assertThat(registry.find(COMPRESSED_CHUNKS_METRIC).gauge().value()).isEqualTo(7.0);
    }

    // ===========================================
    // Null handling tests
    // ===========================================

    @Test
    void testRefreshMetrics_NullTotalChunks_DefaultsToZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(null)
                .thenReturn(4L);

        publisher.refreshMetrics();

        assertThat(registry.find(TOTAL_CHUNKS_METRIC).gauge().value()).isZero();
        assertThat(registry.find(COMPRESSED_CHUNKS_METRIC).gauge().value()).isEqualTo(4.0);
    }

    @Test
    void testRefreshMetrics_NullCompressedChunks_DefaultsToZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(6L)
                .thenReturn(null);

        publisher.refreshMetrics();

        assertThat(registry.find(TOTAL_CHUNKS_METRIC).gauge().value()).isEqualTo(6.0);
        assertThat(registry.find(COMPRESSED_CHUNKS_METRIC).gauge().value()).isZero();
    }

    @Test
    void testRefreshMetrics_BothNull_BothDefaultToZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(null);

        publisher.refreshMetrics();

        assertThat(registry.find(TOTAL_CHUNKS_METRIC).gauge().value()).isZero();
        assertThat(registry.find(COMPRESSED_CHUNKS_METRIC).gauge().value()).isZero();
    }

    // ===========================================
    // Exception handling tests
    // ===========================================

    @Test
    void testRefreshMetrics_DataAccessException_DoesNotPropagate() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenThrow(new DataAccessResourceFailureException("Connection refused"));

        // Should not throw
        publisher.refreshMetrics();

        // Gauges remain at initial value (0)
        assertThat(registry.find(TOTAL_CHUNKS_METRIC).gauge().value()).isZero();
        assertThat(registry.find(COMPRESSED_CHUNKS_METRIC).gauge().value()).isZero();
    }

    @Test
    void testRefreshMetrics_ExceptionAfterFirstQuery_PreservesPartialUpdate() {
        // First call to queryForObject succeeds, second throws
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(5L)
                .thenReturn(3L);

        // First refresh succeeds
        publisher.refreshMetrics();
        assertThat(registry.find(TOTAL_CHUNKS_METRIC).gauge().value()).isEqualTo(5.0);

        // Second refresh fails entirely — values from first refresh remain
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenThrow(new DataAccessResourceFailureException("Timeout"));

        publisher.refreshMetrics();

        assertThat(registry.find(TOTAL_CHUNKS_METRIC).gauge().value()).isEqualTo(5.0);
        assertThat(registry.find(COMPRESSED_CHUNKS_METRIC).gauge().value()).isEqualTo(3.0);
    }
}
