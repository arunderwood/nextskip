package io.nextskip.spots.internal.stream;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nextskip.spots.internal.client.SpotSource;
import io.nextskip.spots.internal.enrichment.ContinentEnricher;
import io.nextskip.spots.internal.enrichment.DistanceEnricher;
import io.nextskip.spots.internal.parser.PskReporterJsonParser;
import io.nextskip.spots.model.Spot;
import io.nextskip.spots.persistence.entity.SpotEntity;
import io.nextskip.spots.persistence.repository.SpotRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.pekko.NotUsed;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.stream.OverflowStrategy;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.stream.javadsl.SourceQueueWithComplete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Pekko Streams processor for PSKReporter spots.
 *
 * <p>Pipeline stages:
 * <ol>
 *   <li><b>Source</b>: MQTT messages from {@link SpotSource}</li>
 *   <li><b>Buffer</b>: 10K elements with dropHead overflow strategy</li>
 *   <li><b>Parse</b>: JSON to Spot via {@link PskReporterJsonParser}</li>
 *   <li><b>Enrich</b>: Add distance and continent via enrichers</li>
 *   <li><b>Batch</b>: Group 100 spots or 1 second timeout</li>
 *   <li><b>Persist</b>: Async batch insert via {@link SpotRepository}</li>
 * </ol>
 *
 * <p>Backpressure is handled at the buffer stage. When the buffer fills,
 * oldest messages are dropped to prioritize recent data.
 */
@Component
@ConditionalOnProperty(prefix = "nextskip.spots", name = "enabled", havingValue = "true", matchIfMissing = true)
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed beans are intentionally shared")
public class SpotStreamProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SpotStreamProcessor.class);

    private final ActorSystem actorSystem;
    private final SpotSource spotSource;
    private final PskReporterJsonParser parser;
    private final DistanceEnricher distanceEnricher;
    private final ContinentEnricher continentEnricher;
    private final SpotRepository spotRepository;

    private final int batchSize;
    private final Duration batchTimeout;
    private final int bufferSize;

    private final AtomicLong spotsProcessed = new AtomicLong(0);
    private final AtomicLong batchesPersisted = new AtomicLong(0);

    @SuppressWarnings("checkstyle:ParameterNumber") // Spring constructor injection with required dependencies
    public SpotStreamProcessor(
            ActorSystem actorSystem,
            SpotSource spotSource,
            PskReporterJsonParser parser,
            DistanceEnricher distanceEnricher,
            ContinentEnricher continentEnricher,
            SpotRepository spotRepository,
            @Value("${nextskip.spots.processing.batch-size:100}") int batchSize,
            @Value("${nextskip.spots.processing.batch-timeout:1s}") Duration batchTimeout,
            @Value("${nextskip.spots.processing.buffer-size:10000}") int bufferSize) {
        this.actorSystem = actorSystem;
        this.spotSource = spotSource;
        this.parser = parser;
        this.distanceEnricher = distanceEnricher;
        this.continentEnricher = continentEnricher;
        this.spotRepository = spotRepository;
        this.batchSize = batchSize;
        this.batchTimeout = batchTimeout;
        this.bufferSize = bufferSize;
    }

    @PostConstruct
    public void start() {
        LOG.info("Starting spot stream processor (batchSize={}, timeout={}, buffer={})",
                batchSize, batchTimeout, bufferSize);

        // Create Pekko queue source with dropHead overflow strategy
        Pair<SourceQueueWithComplete<String>, Source<String, NotUsed>> queuePair =
                Source.<String>queue(bufferSize, OverflowStrategy.dropHead())
                        .preMaterialize(actorSystem);

        SourceQueueWithComplete<String> queue = queuePair.first();

        // Set up message handler to offer to queue
        spotSource.setMessageHandler(message -> {
            queue.offer(message).whenComplete((result, error) -> {
                if (error != null) {
                    LOG.debug("Failed to offer message to queue: {}", error.getMessage());
                }
            });
        });

        // Connect to the spot source
        spotSource.connect();

        // Build the processing pipeline
        queuePair.second()
                // Parse JSON to Spot
                .map(parser::parse)
                .filter(Optional::isPresent)
                .map(Optional::get)
                // Enrich with distance and continent
                .map(distanceEnricher::enrich)
                .map(continentEnricher::enrich)
                // Count processed spots
                .map(spot -> {
                    spotsProcessed.incrementAndGet();
                    return spot;
                })
                // Batch for efficient persistence
                .groupedWithin(batchSize, batchTimeout)
                // Async persist each batch
                .mapAsync(1, this::persistBatchAsync)
                // Run the stream
                .runWith(Sink.ignore(), actorSystem);

        LOG.info("Spot stream processor started");
    }

    @PreDestroy
    public void stop() {
        LOG.info("Stopping spot stream processor. Processed {} spots in {} batches",
                spotsProcessed.get(), batchesPersisted.get());
    }

    private CompletionStage<List<Spot>> persistBatchAsync(List<Spot> spots) {
        return CompletableFuture.supplyAsync(() -> {
            persistBatch(spots);
            return spots;
        });
    }

    @Transactional
    public void persistBatch(List<Spot> spots) {
        if (spots.isEmpty()) {
            return;
        }

        try {
            List<SpotEntity> entities = spots.stream()
                    .map(SpotEntity::fromDomain)
                    .toList();

            spotRepository.saveAll(entities);
            batchesPersisted.incrementAndGet();

            if (LOG.isDebugEnabled()) {
                LOG.debug("Persisted batch of {} spots", spots.size());
            }
        } catch (org.springframework.dao.DataAccessException e) {
            LOG.error("Failed to persist batch of {} spots: {}", spots.size(), e.getMessage());
        }
    }

    /**
     * Returns the total number of spots processed.
     *
     * @return count of spots that passed through the pipeline
     */
    public long getSpotsProcessed() {
        return spotsProcessed.get();
    }

    /**
     * Returns the total number of batches persisted.
     *
     * @return count of batches written to database
     */
    public long getBatchesPersisted() {
        return batchesPersisted.get();
    }
}
