package io.nextskip.common.scheduler;

import org.slf4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abstract base class for data refresh services.
 *
 * <p>Implements the Template Method pattern to provide a consistent structure
 * for all refresh operations while allowing subclasses to define domain-specific logic.
 *
 * <p>This class exists to solve the Spring AOP proxy limitation with db-scheduler:
 * when a {@code @Configuration} class calls its own {@code @Transactional} method
 * via a lambda, the call bypasses the Spring proxy and the transaction is not applied.
 * By extracting the transactional logic to a separate {@code @Service} bean,
 * the proxy is properly invoked.
 *
 * <p>Cache refresh is handled via Spring's event system. After {@link #doRefresh()}
 * completes, a {@link CacheRefreshEvent} is published. The {@link CacheRefreshEventListener}
 * uses {@code @TransactionalEventListener(phase = AFTER_COMMIT)} to execute the cache
 * refresh only after the transaction commits. This prevents the race condition where
 * async cache refresh could query the database before data was visible.
 *
 * <p>Subclasses implement:
 * <ul>
 *   <li>{@link #getServiceName()} - identifier for logging</li>
 *   <li>{@link #doRefresh()} - domain-specific fetch, convert, save, cleanup logic</li>
 *   <li>{@link #createCacheRefreshEvent()} - event with cache refresh action</li>
 *   <li>{@link #getSuccessMessage()} - formatted success log message</li>
 *   <li>{@link #getLog()} - logger instance for the subclass</li>
 * </ul>
 *
 * <p>The base class provides:
 * <ul>
 *   <li>Transaction management via {@code @Transactional}</li>
 *   <li>Post-commit cache refresh via event publishing</li>
 *   <li>Consistent logging pattern</li>
 * </ul>
 *
 * <p>Exception handling is delegated to subclasses, which should catch domain-specific
 * exceptions and wrap them in {@link DataRefreshException} as appropriate.
 */
public abstract class AbstractRefreshService {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Creates a new refresh service with the given event publisher.
     *
     * @param eventPublisher Spring's event publisher for cache refresh events
     */
    protected AbstractRefreshService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Executes the data refresh operation within a transaction.
     *
     * <p>Subclasses customize behavior by implementing the abstract hook methods.
     * Do not override this method; customize via the template hooks instead.
     *
     * <p>The method:
     * <ol>
     *   <li>Logs the start of the refresh</li>
     *   <li>Calls {@link #doRefresh()} for domain-specific logic</li>
     *   <li>Publishes {@link CacheRefreshEvent} (executed after commit)</li>
     *   <li>Logs success via {@link #getSuccessMessage()}</li>
     * </ol>
     *
     * <p>Transaction behavior:
     * <ul>
     *   <li>All database operations in {@link #doRefresh()} are atomic</li>
     *   <li>Any {@link RuntimeException} causes automatic rollback</li>
     *   <li>Subclasses should throw {@link DataRefreshException} for domain errors</li>
     *   <li>Cache refresh is deferred until after transaction commits</li>
     * </ul>
     *
     * @throws DataRefreshException if the refresh operation fails
     */
    @Transactional
    public void executeRefresh() {
        getLog().debug("Executing {} refresh", getServiceName());

        doRefresh();
        eventPublisher.publishEvent(createCacheRefreshEvent());

        getLog().info(getSuccessMessage());
    }

    /**
     * Returns the service name used in log messages.
     *
     * @return the service name (e.g., "SOTA", "POTA", "Contest")
     */
    protected abstract String getServiceName();

    /**
     * Performs the domain-specific refresh logic.
     *
     * <p>Implementations should:
     * <ul>
     *   <li>Fetch data from the external API client</li>
     *   <li>Convert domain models to entities</li>
     *   <li>Persist entities to the database</li>
     *   <li>Clean up old/stale data if applicable</li>
     * </ul>
     *
     * <p>This method runs within the transaction started by {@link #executeRefresh()}.
     *
     * <p>Exception handling: Implementations should catch domain-specific exceptions
     * (e.g., {@link org.springframework.dao.DataAccessException}) and wrap them in
     * {@link DataRefreshException} with a descriptive message.
     *
     * @throws DataRefreshException if the refresh operation fails
     */
    protected abstract void doRefresh();

    /**
     * Creates the cache refresh event with service-specific refresh action.
     *
     * <p>The event encapsulates the cache refresh logic as a {@link Runnable}.
     * This follows the functional strategy pattern - each service knows its own
     * cache and defines how to refresh it.
     *
     * <p>Example implementation:
     * <pre>{@code
     * @Override
     * protected CacheRefreshEvent createCacheRefreshEvent() {
     *     return new CacheRefreshEvent("activations",
     *         () -> activationsCache.refresh(CacheConfig.CACHE_KEY));
     * }
     * }</pre>
     *
     * @return the cache refresh event to publish
     */
    protected abstract CacheRefreshEvent createCacheRefreshEvent();

    /**
     * Returns the success message to log after a successful refresh.
     *
     * <p>Implementations should include relevant metrics like:
     * <ul>
     *   <li>Number of records saved</li>
     *   <li>Number of records deleted</li>
     *   <li>Any other relevant statistics</li>
     * </ul>
     *
     * @return the formatted success message
     */
    protected abstract String getSuccessMessage();

    /**
     * Returns the logger for the concrete subclass.
     *
     * <p>Subclasses should return their own logger instance:
     * <pre>{@code
     * private static final Logger LOG = LoggerFactory.getLogger(MyRefreshService.class);
     *
     * @Override
     * protected Logger getLog() {
     *     return LOG;
     * }
     * }</pre>
     *
     * @return the logger instance
     */
    protected abstract Logger getLog();
}
