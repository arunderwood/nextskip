package io.nextskip.common.scheduler;

/**
 * Exception thrown when a data refresh task fails.
 *
 * <p>Wraps underlying exceptions from data loaders or repositories
 * to provide a consistent exception type for scheduled task failures.
 */
public class DataRefreshException extends RuntimeException {

    /**
     * Creates a new data refresh exception.
     *
     * @param message description of the refresh failure
     * @param cause   the underlying cause of the failure
     */
    public DataRefreshException(String message, Throwable cause) {
        super(message, cause);
    }
}
