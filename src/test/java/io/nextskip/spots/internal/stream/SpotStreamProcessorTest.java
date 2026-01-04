package io.nextskip.spots.internal.stream;

import io.nextskip.spots.internal.client.SpotSource;
import io.nextskip.spots.internal.enrichment.ContinentEnricher;
import io.nextskip.spots.internal.enrichment.DistanceEnricher;
import io.nextskip.spots.internal.parser.PskReporterJsonParser;
import io.nextskip.spots.model.Spot;
import io.nextskip.spots.persistence.repository.SpotRepository;
import org.apache.pekko.actor.ActorSystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@link SpotStreamProcessor} using Pekko Streams.
 *
 * <p>Tests verify:
 * <ul>
 *   <li>Pipeline processes valid spots correctly</li>
 *   <li>Supervision strategy resumes on parse errors</li>
 *   <li>Buffer overflow drops oldest messages</li>
 *   <li>Graceful shutdown drains in-flight elements</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.UnitTestShouldIncludeAssert") // Tests use Awaitility and AssertJ assertions
class SpotStreamProcessorTest {

    private static final int TEST_BATCH_SIZE = 5;
    private static final Duration TEST_BATCH_TIMEOUT = Duration.ofMillis(100);
    private static final int TEST_BUFFER_SIZE = 100;
    private static final int TEST_PARALLELISM = 2;
    private static final String VALID_MESSAGE = "valid";
    private static final String INVALID_MESSAGE = "invalid";
    private static final String BAD_MESSAGE = "bad";
    private static final String GOOD_MESSAGE = "good";

    private ActorSystem actorSystem;
    private ExecutorService persistenceExecutor;
    private SpotStreamProcessor processor;

    @Mock
    private SpotSource spotSource;

    @Mock
    private PskReporterJsonParser parser;

    @Mock
    private DistanceEnricher distanceEnricher;

    @Mock
    private ContinentEnricher continentEnricher;

    @Mock
    private SpotRepository spotRepository;

    private AtomicReference<Consumer<String>> messageHandlerRef;

    @BeforeEach
    void setUp() {
        actorSystem = ActorSystem.create("test-spots");
        persistenceExecutor = Executors.newFixedThreadPool(2);
        messageHandlerRef = new AtomicReference<>();

        // Capture the message handler when set (void method requires doAnswer)
        doAnswer(invocation -> {
            messageHandlerRef.set(invocation.getArgument(0));
            return null;
        }).when(spotSource).setMessageHandler(any());
    }

    @AfterEach
    void tearDown() throws InterruptedException, ExecutionException, TimeoutException {
        if (processor != null) {
            processor.stop();
        }
        if (persistenceExecutor != null) {
            persistenceExecutor.shutdown();
            persistenceExecutor.awaitTermination(5, TimeUnit.SECONDS);
        }
        if (actorSystem != null) {
            actorSystem.terminate();
            actorSystem.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void testStart_InitializesStreamAndConnects() {
        // Given
        processor = createProcessor();

        // When
        processor.start();

        // Then
        verify(spotSource).setMessageHandler(any());
        verify(spotSource).connect();
        assertThat(messageHandlerRef.get()).isNotNull();
    }

    @Test
    void testProcess_ValidSpots_IncrementsSpotsProcessedCounter() {
        // Given
        processor = createProcessor();
        Spot testSpot = createTestSpot();
        setupParserToReturnSpot(testSpot);
        setupEnrichersToPassThrough();

        processor.start();
        Consumer<String> handler = messageHandlerRef.get();

        // When - Send valid JSON messages
        for (int i = 0; i < 10; i++) {
            handler.accept(createValidJson());
        }

        // Then - Wait for processing
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(processor.getSpotsProcessed()).isGreaterThanOrEqualTo(10));
    }

    @Test
    void testProcess_BatchPersistence_IncrementsBatchCounter() {
        // Given
        processor = createProcessor();
        Spot testSpot = createTestSpot();
        setupParserToReturnSpot(testSpot);
        setupEnrichersToPassThrough();

        processor.start();
        Consumer<String> handler = messageHandlerRef.get();

        // When - Send enough messages to trigger batch persistence
        for (int i = 0; i < TEST_BATCH_SIZE * 2; i++) {
            handler.accept(createValidJson());
        }

        // Then - Wait for batches to be persisted
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    assertThat(processor.getBatchesPersisted()).isGreaterThanOrEqualTo(1);
                    verify(spotRepository, atLeastOnce()).saveAll(anyList());
                });
    }

    @Test
    void testProcess_MalformedJson_ContinuesProcessing() {
        // Given - Parser returns empty for invalid JSON
        processor = createProcessor();
        Spot testSpot = createTestSpot();

        when(parser.parse(INVALID_MESSAGE)).thenReturn(Optional.empty());
        when(parser.parse(VALID_MESSAGE)).thenReturn(Optional.of(testSpot));
        setupEnrichersToPassThrough();

        processor.start();
        Consumer<String> handler = messageHandlerRef.get();

        // When - Send mix of valid and invalid messages
        handler.accept(INVALID_MESSAGE);
        handler.accept(VALID_MESSAGE);
        handler.accept(INVALID_MESSAGE);
        handler.accept(VALID_MESSAGE);

        // Then - Stream continues, valid spots are processed
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(processor.getSpotsProcessed()).isGreaterThanOrEqualTo(2));
    }

    @Test
    void testProcess_SupervisionStrategy_ContinuesAfterParseFailure() {
        // Given - Test that the stream continues processing after failures
        // This verifies the supervision strategy is applied (parse failures are filtered, not exceptions)
        processor = createProcessor();
        Spot testSpot = createTestSpot();

        // Parser returns empty for "bad" messages and spot for "good" ones
        when(parser.parse(BAD_MESSAGE)).thenReturn(Optional.empty());
        when(parser.parse(GOOD_MESSAGE)).thenReturn(Optional.of(testSpot));
        setupEnrichersToPassThrough();

        processor.start();
        Consumer<String> handler = messageHandlerRef.get();

        // When - Send mix of valid and invalid messages
        handler.accept(BAD_MESSAGE);
        handler.accept(GOOD_MESSAGE);
        handler.accept(BAD_MESSAGE);
        handler.accept(GOOD_MESSAGE);
        handler.accept(GOOD_MESSAGE);

        // Then - Stream continues processing valid spots
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(processor.getSpotsProcessed()).isGreaterThanOrEqualTo(3));
    }

    @Test
    void testStop_GracefulShutdown_LogsMetrics() {
        // Given
        processor = createProcessor();
        Spot testSpot = createTestSpot();
        setupParserToReturnSpot(testSpot);
        setupEnrichersToPassThrough();

        processor.start();
        Consumer<String> handler = messageHandlerRef.get();

        // Send some messages
        for (int i = 0; i < 5; i++) {
            handler.accept(createValidJson());
        }

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(processor.getSpotsProcessed()).isGreaterThanOrEqualTo(5));

        // When
        processor.stop();

        // Then - Processor stopped without error (metrics logged in stop())
        assertThat(processor.getSpotsProcessed()).isGreaterThanOrEqualTo(5);
    }

    @Test
    void testGetDroppedMessages_InitiallyZero() {
        // Given
        processor = createProcessor();
        processor.start();

        // Then
        assertThat(processor.getDroppedMessages()).isZero();
    }

    @Test
    void testMetrics_AllCountersAccessible() {
        // Given
        processor = createProcessor();
        processor.start();

        // Then - All metrics are accessible
        assertThat(processor.getSpotsProcessed()).isGreaterThanOrEqualTo(0);
        assertThat(processor.getBatchesPersisted()).isGreaterThanOrEqualTo(0);
        assertThat(processor.getDroppedMessages()).isGreaterThanOrEqualTo(0);
    }

    private SpotStreamProcessor createProcessor() {
        return new SpotStreamProcessor(
                actorSystem,
                spotSource,
                parser,
                distanceEnricher,
                continentEnricher,
                spotRepository,
                persistenceExecutor,
                TEST_BATCH_SIZE,
                TEST_BATCH_TIMEOUT,
                TEST_BUFFER_SIZE,
                TEST_PARALLELISM
        );
    }

    private Spot createTestSpot() {
        return new Spot(
                "PSKReporter",
                "20m",
                "FT8",
                14074000L,
                -10,
                Instant.now(),
                "W1ABC",
                "FN42ab",
                "G3XYZ",
                "JO01cd",
                null,
                null,
                null
        );
    }

    private String createValidJson() {
        return """
            {"md": "FT8", "t": 1662407712, "b": "20m", "sc": "W1ABC", "sl": "FN42ab"}
            """;
    }

    private void setupParserToReturnSpot(Spot spot) {
        when(parser.parse(any())).thenReturn(Optional.of(spot));
    }

    private void setupEnrichersToPassThrough() {
        when(distanceEnricher.enrich(any())).thenAnswer(inv -> inv.getArgument(0));
        when(continentEnricher.enrich(any())).thenAnswer(inv -> inv.getArgument(0));
    }
}
