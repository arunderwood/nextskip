package io.nextskip.common.scheduler;

import org.slf4j.Logger;
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
 * <p>Subclasses implement:
 * <ul>
 *   <li>{@link #getServiceName()} - identifier for logging</li>
 *   <li>{@link #doRefresh()} - domain-specific fetch, convert, save, cleanup logic</li>
 *   <li>{@link #refreshCache()} - trigger cache refresh after DB write</li>
 *   <li>{@link #getSuccessMessage()} - formatted success log message</li>
 *   <li>{@link #getLog()} - logger instance for the subclass</li>
 * </ul>
 *
 * <p>The base class provides:
 * <ul>
 *   <li>Transaction management via {@code @Transactional}</li>
 *   <li>Consistent logging pattern</li>
 * </ul>
 *
 * <p>Exception handling is delegated to subclasses, which should catch domain-specific
 * exceptions and wrap them in {@link DataRefreshException} as appropriate.
 */
public abstract class AbstractRefreshService {

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
     *   <li>Calls {@link #refreshCache()} to update the application cache</li>
     *   <li>Logs success via {@link #getSuccessMessage()}</li>
     * </ol>
     *
     * <p>Transaction behavior:
     * <ul>
     *   <li>All database operations in {@link #doRefresh()} are atomic</li>
     *   <li>Any {@link RuntimeException} causes automatic rollback</li>
     *   <li>Subclasses should throw {@link DataRefreshException} for domain errors</li>
     * </ul>
     *
     * @throws DataRefreshException if the refresh operation fails
     */
    @Transactional
    public void executeRefresh() {
        getLog().debug("Executing {} refresh", getServiceName());

        doRefresh();
        refreshCache();

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
     * Triggers an async cache refresh after the database write.
     *
     * <p>Typically calls {@code cache.refresh(key)} on the application's
     * {@link com.github.benmanes.caffeine.cache.LoadingCache}.
     */
    protected abstract void refreshCache();

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
