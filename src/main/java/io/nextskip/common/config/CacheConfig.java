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
 * - potaActivations: 2 minutes (POTA real-time spots)
 * - sotaActivations: 2 minutes (SOTA real-time spots)
 * - meteorShowers: 2 hours (meteor shower data)
 * - contests: 6 hours refresh (contest schedules are stable)
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

        cacheManager.registerCustomCache("contests",
            Caffeine.newBuilder()
                .expireAfterWrite(6, TimeUnit.HOURS)
                .maximumSize(200)
                .recordStats()
                .build());

        cacheManager.registerCustomCache("potaActivations",
            Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .maximumSize(100)
                .recordStats()
                .build());

        cacheManager.registerCustomCache("sotaActivations",
            Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .maximumSize(100)
                .recordStats()
                .build());

        cacheManager.registerCustomCache("meteorShowers",
            Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.HOURS)
                .maximumSize(50)
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
