package io.nextskip.activations.internal.scheduler;

import io.nextskip.activations.persistence.entity.ActivationEntity;
import io.nextskip.activations.persistence.repository.ActivationRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helper for upserting activation entities.
 *
 * <p>Provides merge semantics by looking up existing entities and setting their IDs
 * on incoming entities, allowing JPA's saveAll to perform UPDATE instead of INSERT
 * for existing records.
 *
 * <p>This approach uses a selective lookup (only fetching entities matching incoming
 * spotIds) rather than loading all entities from a source, which is more efficient
 * for large datasets.
 */
final class ActivationUpsertHelper {

    private ActivationUpsertHelper() {
        // Utility class - prevent instantiation
    }

    /**
     * Prepares entities for upsert by setting IDs from existing database records.
     *
     * <p>Entities with matching (spotId, source) will have their ID set, causing
     * JPA to UPDATE rather than INSERT. New entities keep null ID for INSERT.
     *
     * <p>Performance: Uses a single SELECT with IN clause to fetch only entities
     * matching the incoming spotIds, avoiding loading all historical data.
     *
     * @param entities   the entities to prepare
     * @param source     the data source (e.g., "POTA API", "SOTA API")
     * @param repository the repository for looking up existing entities
     */
    static void prepareForUpsert(
            List<ActivationEntity> entities,
            String source,
            ActivationRepository repository) {

        if (entities.isEmpty()) {
            return;
        }

        // Extract spotIds for selective lookup
        List<String> spotIds = entities.stream()
                .map(ActivationEntity::getSpotId)
                .toList();

        // Selective fetch - only entities we're about to update
        Map<String, Long> existingIds = repository
                .findBySourceAndSpotIdIn(source, spotIds)
                .stream()
                .collect(Collectors.toMap(
                        ActivationEntity::getSpotId,
                        ActivationEntity::getId,
                        (existing, replacement) -> existing // Handle unlikely duplicates
                ));

        // Set ID on entities that already exist (enables UPDATE vs INSERT)
        for (ActivationEntity entity : entities) {
            Long existingId = existingIds.get(entity.getSpotId());
            if (existingId != null) {
                entity.setId(existingId);
            }
        }
    }
}
