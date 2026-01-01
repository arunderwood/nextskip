package io.nextskip.common.scheduler;

/**
 * Event published after data refresh to trigger cache reload.
 *
 * <p>This event follows the functional strategy pattern - it carries its own refresh action
 * as a {@link Runnable}. The listener is completely generic and never needs modification
 * when new cache types are added.
 *
 * <p>Published within a {@code @Transactional} method, this event is handled by
 * {@link CacheRefreshEventListener} which uses {@code @TransactionalEventListener(phase = AFTER_COMMIT)}
 * to ensure cache refresh happens after the database transaction commits.
 *
 * <p>This solves the race condition where cache refresh triggered within a transaction
 * would query the database before data was committed.
 *
 * @param cacheName descriptive name for logging (e.g., "activations", "solarIndices")
 * @param refreshAction the cache refresh operation to execute post-commit
 */
public record CacheRefreshEvent(String cacheName, Runnable refreshAction) {
}
