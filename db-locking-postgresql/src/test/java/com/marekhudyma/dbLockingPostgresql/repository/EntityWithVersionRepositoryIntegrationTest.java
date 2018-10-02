package com.marekhudyma.dbLockingPostgresql.repository;

import com.marekhudyma.dbLockingPostgresql.model.EntityWithVersion;
import com.marekhudyma.dbLockingPostgresql.utils.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class EntityWithVersionRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EntityWithVersionRepository entityWithVersionRepository;

    @Autowired
    private EntityWithVersionOptimisticRepository entityWithVersionOptimisticRepository;

    @Test
    void shouldSaveAndFindOperation() throws Exception {
        EntityWithVersion entity = create();

        EntityWithVersion actual = entityWithVersionRepository.findById(entity.getId()).get();

        assertThat(actual).isEqualTo(entity);
    }

    @Test
    void shouldIncrementVersion() throws Exception {
        EntityWithVersion entity = create();
        entity.setDescription("description-changed");
        entityWithVersionRepository.save(entity);

        entity = entityWithVersionRepository.findById(entity.getId()).get();
        entity.setDescription("description-changed2");
        entityWithVersionRepository.save(entity);

        EntityWithVersion actual = entityWithVersionRepository.findById(entity.getId()).get();
        assertThat(actual.getVersion()).isEqualTo(2);
    }

    @Test
    void shouldThrownObjectOptimisticLockingFailureExceptionForSecondSave() throws Exception {
        EntityWithVersion entity = create();
        entity.setDescription("description-changed");

        entityWithVersionRepository.save(entity);

        assertThatThrownBy(() -> entityWithVersionRepository.save(entity))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    private EntityWithVersion create() {
        EntityWithVersion entity = EntityWithVersion.builder()
                .description("description")
                .build();
        return entityWithVersionRepository.save(entity);
    }

}
