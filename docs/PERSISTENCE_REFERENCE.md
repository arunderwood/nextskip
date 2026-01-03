# Persistence Reference

Quick reference for adding data source integrations with persistence.

## Architecture

```
External API → FeedClient.fetch() → RefreshTask → Database → cache.refresh("all")
                      ↓ (throws on failure)           ↓                ↓
                Scheduler reschedules        LoadingCache serves DB data
                                                      ↓
               Browser ← @BrowserCallable ← Service ← LoadingCache
```

**Key principle**: FeedClients do not cache—they fetch and throw on failure. The database
serves as the single source of truth, with LoadingCache providing fast reads.

## Reference Examples

| Component | Reference File | Key Pattern |
|-----------|----------------|-------------|
| Domain Model | `propagation/model/SolarIndices.java` | Immutable Java record |
| FeedClient | `propagation/internal/NoaaSwpcClient.java` | Circuit breaker + retry, no caching |
| Entity (simple) | `propagation/persistence/entity/SolarIndicesEntity.java` | `fromDomain()` / `toDomain()` |
| Entity (complex) | `activations/persistence/entity/ActivationEntity.java` | Polymorphic flattening |
| Entity (collections) | `contests/persistence/entity/ContestEntity.java` | `@ElementCollection` |
| Repository | `propagation/persistence/repository/SolarIndicesRepository.java` | Spring Data JPA |
| Migration | `db/changelog/migrations/002-solar-indices-table.yaml` | Liquibase YAML |
| Refresh Task | `propagation/internal/scheduler/NoaaRefreshTask.java` | db-scheduler `RecurringTask` |
| Cache Config | `common/config/CacheConfig.java` | Caffeine `LoadingCache` |
| Endpoint | `propagation/api/PropagationEndpoint.java` | `@BrowserCallable` |

All paths relative to `src/main/java/io/nextskip/` (or `src/main/resources/` for migrations).

## Conventions

**Migration naming:** `XXX-description.yaml` (zero-padded sequence, e.g., `008-my-table.yaml`)

**Task naming:** `<module>-refresh` or `<source>-refresh` (e.g., `noaa-refresh`, `pota-refresh`)

**Cache key:** Use `CacheConfig.CACHE_KEY` (`"all"`) for single-list caches

## Checklist

- [ ] Domain model is immutable (Java record)
- [ ] Entity has `fromDomain()` and `toDomain()` converters
- [ ] Entity has `protected` no-arg constructor
- [ ] Migration follows naming convention
- [ ] Refresh task uses `@Transactional` and throws `DataRefreshException`
- [ ] `LoadingCache` bean added to `CacheConfig`
- [ ] Endpoint uses `@BrowserCallable` and `@AnonymousAllowed`
- [ ] Unit tests for entity converters and refresh task
