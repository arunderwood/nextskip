package io.nextskip.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration using Caffeine.
 *
 * Defines cache TTLs for different data sources:
 * - solarIndices: 5 minutes (NOAA data)
 * - bandConditions: 30 minutes (HamQSL data)
 * - tleData: 6 hours (Satellite TLE data)
 * - default: 10 minutes
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(defaultCaffeine());

        // Register caches with specific TTLs
        cacheManager.registerCustomCache("solarIndices",
            Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(100)
                .recordStats()
                .build());

        cacheManager.registerCustomCache("bandConditions",
            Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(100)
                .recordStats()
                .build());

        cacheManager.registerCustomCache("tleData",
            Caffeine.newBuilder()
                .expireAfterWrite(6, TimeUnit.HOURS)
                .maximumSize(1000)
                .recordStats()
                .build());

        cacheManager.registerCustomCache("activations",
            Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(500)
                .recordStats()
                .build());

        return cacheManager;
    }

    private Caffeine<Object, Object> defaultCaffeine() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats();
    }
}
