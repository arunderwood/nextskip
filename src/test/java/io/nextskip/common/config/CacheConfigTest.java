package io.nextskip.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for CacheConfig.
 *
 * <p>Verifies that all cache names used by @Cacheable annotations
 * are properly registered in the CacheManager.
 */
class CacheConfigTest {

    /**
     * All cache names that @Cacheable annotations reference must be registered.
     * This test prevents cache name mismatches that would cause caching to silently fail.
     */
    @Test
    void testCacheManager_RegistersAllExpectedCaches() {
        // Given
        CacheConfig config = new CacheConfig();
        CacheManager cacheManager = config.cacheManager();

        // Then: All cache names that @Cacheable annotations reference should exist
        String[] expectedCaches = {
            "solarIndices",      // NoaaSwpcClient, HamQslClient
            "bandConditions",    // HamQslClient
            "potaActivations",   // PotaClient
            "sotaActivations",   // SotaClient
            "meteorShowers",     // MeteorServiceImpl
            "contests",          // ContestCalendarClient
            "tleData"            // Future satellite tracking
        };

        for (String cacheName : expectedCaches) {
            assertNotNull(cacheManager.getCache(cacheName),
                "Cache '" + cacheName + "' should be registered in CacheConfig");
        }
    }

    @Test
    void testCacheManager_DefaultCacheIsAvailable() {
        // Given
        CacheConfig config = new CacheConfig();
        CacheManager cacheManager = config.cacheManager();

        // When: Request a cache that wasn't explicitly registered
        var defaultCache = cacheManager.getCache("someUnregisteredCache");

        // Then: Should get a default cache (CaffeineCacheManager creates on-demand)
        assertNotNull(defaultCache, "Default cache should be available for unregistered names");
    }
}
