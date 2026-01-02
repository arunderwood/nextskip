package io.nextskip.activations.internal.scheduler;

import io.nextskip.activations.model.ActivationType;
import io.nextskip.activations.persistence.entity.ActivationEntity;
import io.nextskip.activations.persistence.repository.ActivationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ActivationUpsertHelper.
 */
@ExtendWith(MockitoExtension.class)
class ActivationUpsertHelperTest {

    private static final String SOURCE_POTA = "POTA API";

    @Mock
    private ActivationRepository repository;

    @Test
    void testPrepareForUpsert_EmptyList_DoesNotQueryRepository() {
        List<ActivationEntity> entities = Collections.emptyList();

        ActivationUpsertHelper.prepareForUpsert(entities, SOURCE_POTA, repository);

        verify(repository, never()).findBySourceAndSpotIdIn(eq(SOURCE_POTA), anyList());
    }

    @Test
    void testPrepareForUpsert_NewEntity_KeepsNullId() {
        ActivationEntity newEntity = createEntity("spot-new", null);
        List<ActivationEntity> entities = new ArrayList<>(List.of(newEntity));
        when(repository.findBySourceAndSpotIdIn(eq(SOURCE_POTA), anyList()))
                .thenReturn(Collections.emptyList());

        ActivationUpsertHelper.prepareForUpsert(entities, SOURCE_POTA, repository);

        assertThat(entities.get(0).getId()).isNull();
    }

    @Test
    void testPrepareForUpsert_ExistingEntity_SetsId() {
        ActivationEntity existingInDb = createEntity("spot-123", 42L);
        ActivationEntity incoming = createEntity("spot-123", null);
        List<ActivationEntity> entities = new ArrayList<>(List.of(incoming));
        when(repository.findBySourceAndSpotIdIn(eq(SOURCE_POTA), anyList()))
                .thenReturn(List.of(existingInDb));

        ActivationUpsertHelper.prepareForUpsert(entities, SOURCE_POTA, repository);

        assertThat(entities.get(0).getId()).isEqualTo(42L);
    }

    @Test
    void testPrepareForUpsert_MixedEntities_SetsCorrectIds() {
        ActivationEntity existingInDb = createEntity("spot-existing", 100L);
        ActivationEntity incomingExisting = createEntity("spot-existing", null);
        ActivationEntity incomingNew = createEntity("spot-new", null);
        List<ActivationEntity> entities = new ArrayList<>(List.of(incomingExisting, incomingNew));
        when(repository.findBySourceAndSpotIdIn(eq(SOURCE_POTA), anyList()))
                .thenReturn(List.of(existingInDb));

        ActivationUpsertHelper.prepareForUpsert(entities, SOURCE_POTA, repository);

        assertThat(entities.get(0).getId()).isEqualTo(100L);  // existing - gets ID
        assertThat(entities.get(1).getId()).isNull();          // new - keeps null
    }

    @Test
    void testPrepareForUpsert_MultipleExisting_SetsAllIds() {
        ActivationEntity existing1 = createEntity("spot-1", 10L);
        ActivationEntity existing2 = createEntity("spot-2", 20L);
        ActivationEntity incoming1 = createEntity("spot-1", null);
        ActivationEntity incoming2 = createEntity("spot-2", null);
        List<ActivationEntity> entities = new ArrayList<>(List.of(incoming1, incoming2));
        when(repository.findBySourceAndSpotIdIn(eq(SOURCE_POTA), anyList()))
                .thenReturn(List.of(existing1, existing2));

        ActivationUpsertHelper.prepareForUpsert(entities, SOURCE_POTA, repository);

        assertThat(entities.get(0).getId()).isEqualTo(10L);
        assertThat(entities.get(1).getId()).isEqualTo(20L);
    }

    @Test
    void testPrepareForUpsert_QueriesWithCorrectSpotIds() {
        ActivationEntity entity1 = createEntity("spot-abc", null);
        ActivationEntity entity2 = createEntity("spot-xyz", null);
        List<ActivationEntity> entities = new ArrayList<>(List.of(entity1, entity2));
        when(repository.findBySourceAndSpotIdIn(eq(SOURCE_POTA), anyList()))
                .thenReturn(Collections.emptyList());

        ActivationUpsertHelper.prepareForUpsert(entities, SOURCE_POTA, repository);

        verify(repository).findBySourceAndSpotIdIn(
                eq(SOURCE_POTA),
                eq(List.of("spot-abc", "spot-xyz"))
        );
    }

    @Test
    void testPrepareForUpsert_DifferentSource_QueriesCorrectSource() {
        String sotaSource = "SOTA API";
        ActivationEntity entity = createEntity("spot-sota", null);
        List<ActivationEntity> entities = new ArrayList<>(List.of(entity));
        when(repository.findBySourceAndSpotIdIn(eq(sotaSource), anyList()))
                .thenReturn(Collections.emptyList());

        ActivationUpsertHelper.prepareForUpsert(entities, sotaSource, repository);

        verify(repository).findBySourceAndSpotIdIn(eq(sotaSource), anyList());
    }

    private ActivationEntity createEntity(String spotId, Long id) {
        Instant now = Instant.now();
        ActivationEntity entity = new ActivationEntity(
                spotId,
                "W1ABC",
                ActivationType.POTA,
                14074.0,
                "FT8",
                now,
                now, // lastSeenAt
                5,
                SOURCE_POTA,
                "K-1234",
                "Test Park",
                "CO",
                "US",
                "DM79",
                40.0,
                -105.0,
                null
        );
        if (id != null) {
            entity.setId(id);
        }
        return entity;
    }
}
