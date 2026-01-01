package io.nextskip.common.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener that executes cache refresh actions after database transactions commit.
 *
 * <p>This listener uses {@code @TransactionalEventListener(phase = AFTER_COMMIT)} to ensure
 * cache refresh happens only after the database transaction has successfully committed.
 * This solves the race condition where async cache refresh could query the database
 * before uncommitted data was visible.
 *
 * <p>The listener is completely generic - it simply executes the {@link Runnable} provided
 * by the {@link CacheRefreshEvent}. This follows the Open/Closed Principle: adding new
 * cache types requires no changes to this listener.
 */
@Component
public class CacheRefreshEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(CacheRefreshEventListener.class);

    /**
     * Handles cache refresh events after transaction commit.
     *
     * <p>Executes the refresh action encapsulated in the event. This design ensures:
     * <ul>
     *   <li>Database data is committed and visible before cache refresh</li>
     *   <li>Listener is decoupled from specific cache types</li>
     *   <li>Each refresh service defines its own refresh logic</li>
     * </ul>
     *
     * @param event the cache refresh event containing the refresh action
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCacheRefresh(CacheRefreshEvent event) {
        LOG.debug("Refreshing {} cache after transaction commit", event.cacheName());
        event.refreshAction().run();
    }
}
