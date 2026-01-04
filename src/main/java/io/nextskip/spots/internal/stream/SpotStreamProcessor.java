package io.nextskip.spots.internal.stream;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nextskip.spots.internal.client.SpotSource;
import io.nextskip.spots.internal.enrichment.CallsignEnricher;
import io.nextskip.spots.internal.enrichment.ContinentEnricher;
import io.nextskip.spots.internal.enrichment.DistanceEnricher;
import io.nextskip.spots.internal.parser.PskReporterJsonParser;
import io.nextskip.spots.model.Spot;
import io.nextskip.spots.persistence.entity.SpotEntity;
import io.nextskip.spots.persistence.repository.SpotRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.pekko.Done;
import org.apache.pekko.NotUsed;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.japi.function.Function;
import org.apache.pekko.stream.ActorAttributes;
import org.apache.pekko.stream.KillSwitches;
import org.apache.pekko.stream.OverflowStrategy;
import org.apache.pekko.stream.QueueOfferResult;
import org.apache.pekko.stream.Supervision;
import org.apache.pekko.stream.UniqueKillSwitch;
import org.apache.pekko.stream.javadsl.Keep;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
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
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 10;

    private final ActorSystem actorSystem;
    private final SpotSource spotSource;
    private final PskReporterJsonParser parser;
    private final CallsignEnricher callsignEnricher;
    private final DistanceEnricher distanceEnricher;
    private final ContinentEnricher continentEnricher;
    private final SpotRepository spotRepository;
    private final ExecutorService persistenceExecutor;

    private final int batchSize;
    private final Duration batchTimeout;
    private final int bufferSize;
    private final int persistenceParallelism;

    private final AtomicLong spotsProcessed = new AtomicLong(0);
    private final AtomicLong batchesPersisted = new AtomicLong(0);
    private final AtomicLong droppedMessages = new AtomicLong(0);

    private volatile UniqueKillSwitch killSwitch;
    private volatile CompletionStage<Done> streamCompletion;

    /**
     * Supervision strategy that resumes processing on transient errors.
     * Logs the error and continues with the next element.
     */
    private final Function<Throwable, Supervision.Directive> supervisionDecider = exception -> {
        LOG.error("Stream processing error (resuming): {}", exception.getMessage(), exception);
        return Supervision.resume();
    };

    @SuppressWarnings("checkstyle:ParameterNumber") // Spring constructor injection with required dependencies
    public SpotStreamProcessor(
            ActorSystem actorSystem,
            SpotSource spotSource,
            PskReporterJsonParser parser,
            CallsignEnricher callsignEnricher,
            DistanceEnricher distanceEnricher,
            ContinentEnricher continentEnricher,
            SpotRepository spotRepository,
            ExecutorService spotPersistenceExecutor,
            @Value("${nextskip.spots.processing.batch-size:100}") int batchSize,
            @Value("${nextskip.spots.processing.batch-timeout:1s}") Duration batchTimeout,
            @Value("${nextskip.spots.processing.buffer-size:10000}") int bufferSize,
            @Value("${nextskip.spots.processing.persistence-parallelism:2}") int persistenceParallelism) {
        this.actorSystem = actorSystem;
        this.spotSource = spotSource;
        this.parser = parser;
        this.callsignEnricher = callsignEnricher;
        this.distanceEnricher = distanceEnricher;
        this.continentEnricher = continentEnricher;
        this.spotRepository = spotRepository;
        this.persistenceExecutor = spotPersistenceExecutor;
        this.batchSize = batchSize;
        this.batchTimeout = batchTimeout;
        this.bufferSize = bufferSize;
        this.persistenceParallelism = persistenceParallelism;
    }

    @PostConstruct
    public void start() {
        LOG.info("Starting spot stream processor (batchSize={}, timeout={}, buffer={}, parallelism={})",
                batchSize, batchTimeout, bufferSize, persistenceParallelism);

        // Create Pekko queue source with dropHead overflow strategy
        Pair<SourceQueueWithComplete<String>, Source<String, NotUsed>> queuePair =
                Source.<String>queue(bufferSize, OverflowStrategy.dropHead())
                        .preMaterialize(actorSystem);

        SourceQueueWithComplete<String> queue = queuePair.first();

        // Set up message handler to offer to queue with drop tracking
        spotSource.setMessageHandler(message -> {
            queue.offer(message).whenComplete((result, error) -> {
                if (error != null) {
                    LOG.debug("Failed to offer message to queue: {}", error.getMessage());
                } else if (QueueOfferResult.dropped().equals(result)) {
                    droppedMessages.incrementAndGet();
                }
            });
        });

        // Connect to the spot source
        spotSource.connect();

        // Build the processing pipeline with supervision strategy and KillSwitch
        Pair<UniqueKillSwitch, CompletionStage<Done>> materialized = queuePair.second()
                // Apply supervision strategy to resume on transient errors
                .withAttributes(ActorAttributes.withSupervisionStrategy(supervisionDecider))
                // Add KillSwitch for graceful shutdown
                .viaMat(KillSwitches.single(), Keep.right())
                // Parse JSON to Spot
                .map(parser::parse)
                .filter(Optional::isPresent)
                .map(Optional::get)
                // Validate callsigns (logs failures but keeps all spots)
                .map(callsignEnricher::enrich)
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
                // Async persist each batch with configurable parallelism (unordered for throughput)
                .mapAsyncUnordered(persistenceParallelism, this::persistBatchAsync)
                // Run the stream and capture both KillSwitch and completion
                .toMat(Sink.ignore(), Keep.both())
                .run(actorSystem);

        this.killSwitch = materialized.first();
        this.streamCompletion = materialized.second();

        LOG.info("Spot stream processor started");
    }

    @PreDestroy
    @SuppressWarnings("PMD.AvoidCatchingGenericException") // Graceful shutdown handling
    public void stop() {
        LOG.info("Stopping spot stream processor. Processed {} spots in {} batches (dropped {})",
                spotsProcessed.get(), batchesPersisted.get(), droppedMessages.get());

        // Gracefully shutdown the stream via KillSwitch
        if (killSwitch != null) {
            LOG.debug("Initiating graceful stream shutdown");
            killSwitch.shutdown();

            // Wait for in-flight elements to complete
            if (streamCompletion != null) {
                try {
                    streamCompletion.toCompletableFuture()
                            .get(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    LOG.debug("Stream shutdown completed gracefully");
                } catch (Exception e) {
                    LOG.warn("Stream shutdown did not complete within {}s: {}",
                            SHUTDOWN_TIMEOUT_SECONDS, e.getMessage());
                }
            }
        }
    }

    private CompletionStage<List<Spot>> persistBatchAsync(List<Spot> spots) {
        return CompletableFuture.supplyAsync(() -> {
            persistBatch(spots);
            return spots;
        }, persistenceExecutor);
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

    /**
     * Returns the total number of messages dropped due to buffer overflow.
     *
     * <p>When the buffer fills up, the oldest messages are dropped (dropHead strategy)
     * to prioritize recent data. This counter tracks how many messages were lost.
     *
     * @return count of messages dropped from the buffer
     */
    public long getDroppedMessages() {
        return droppedMessages.get();
    }
}
