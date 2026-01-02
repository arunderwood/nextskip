package io.nextskip.common.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for CacheRefreshEventListener.
 */
@ExtendWith(MockitoExtension.class)
class CacheRefreshEventListenerTest {

    @InjectMocks
    private CacheRefreshEventListener listener;

    @Test
    void testOnCacheRefresh_ExecutesRefreshAction() {
        // Given: A mock refresh action
        Runnable mockAction = mock(Runnable.class);
        CacheRefreshEvent event = new CacheRefreshEvent("testCache", mockAction);

        // When: The event is handled
        listener.onCacheRefresh(event);

        // Then: The refresh action is executed
        verify(mockAction).run();
    }

    @Test
    void testOnCacheRefresh_HandlesNoOpAction_DoesNotThrow() {
        // Given: A no-op refresh action (as used when data is skipped)
        CacheRefreshEvent event = new CacheRefreshEvent("testCache (skipped)", () -> { });

        // When/Then: The event is handled without error
        assertThatCode(() -> listener.onCacheRefresh(event))
                .doesNotThrowAnyException();
    }
}
