package io.nextskip.spots.internal;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Publishes TimescaleDB hypertable metrics to Micrometer.
 *
 * <p>Periodically queries TimescaleDB information views and exposes
 * chunk counts as Prometheus gauges via the existing Micrometer pipeline.
 * These metrics flow to Grafana Cloud automatically.
 */
@Component
@ConditionalOnProperty(prefix = "nextskip.spots", name = "enabled", havingValue = "true", matchIfMissing = true)
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed beans are intentionally shared")
public class TimescaleMetricsPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(TimescaleMetricsPublisher.class);

    private final JdbcTemplate jdbcTemplate;
    private final AtomicLong totalChunks = new AtomicLong();
    private final AtomicLong compressedChunks = new AtomicLong();

    public TimescaleMetricsPublisher(JdbcTemplate jdbcTemplate, MeterRegistry registry) {
        this.jdbcTemplate = jdbcTemplate;

        Gauge.builder("nextskip.timescaledb.chunks.total", totalChunks, AtomicLong::doubleValue)
                .description("Total number of TimescaleDB chunks for the spots hypertable")
                .tag("hypertable", "spots")
                .register(registry);

        Gauge.builder("nextskip.timescaledb.chunks.compressed", compressedChunks, AtomicLong::doubleValue)
                .description("Number of compressed TimescaleDB chunks for the spots hypertable")
                .tag("hypertable", "spots")
                .register(registry);
    }

    /**
     * Refreshes chunk metrics every 5 minutes.
     */
    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT30S")
    public void refreshMetrics() {
        try {
            Long total = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM timescaledb_information.chunks WHERE hypertable_name = 'spots'",
                    Long.class);
            totalChunks.set(total != null ? total : 0);

            Long compressed = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM timescaledb_information.chunks "
                            + "WHERE hypertable_name = 'spots' AND is_compressed = true",
                    Long.class);
            compressedChunks.set(compressed != null ? compressed : 0);

            LOG.debug("TimescaleDB metrics: {} total chunks, {} compressed", totalChunks.get(), compressedChunks.get());
        } catch (org.springframework.dao.DataAccessException e) {
            LOG.warn("Failed to refresh TimescaleDB metrics: {}", e.getMessage());
        }
    }
}
