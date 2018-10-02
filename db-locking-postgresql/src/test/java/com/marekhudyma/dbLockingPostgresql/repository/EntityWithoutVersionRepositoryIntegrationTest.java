package com.marekhudyma.dbLockingPostgresql.repository;

import com.marekhudyma.dbLockingPostgresql.model.EntityWithoutVersion;
import com.marekhudyma.dbLockingPostgresql.utils.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;


class EntityWithoutVersionRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EntityWithoutVersionRepository entityWithoutVersionRepository;

    @Test
    void shouldSaveAndFindOperation() throws Exception {
        EntityWithoutVersion entity = create();

        EntityWithoutVersion actual = entityWithoutVersionRepository.findById(entity.getId()).get();

        assertThat(actual).isEqualTo(entity);
    }

    @Test
    void shouldNotThrownExceptionForSecondSave() throws Exception {
        EntityWithoutVersion entity = create();
        entity.setDescription("description-changed");

        entityWithoutVersionRepository.save(entity);
        entityWithoutVersionRepository.save(entity);
    }

    private EntityWithoutVersion create() {
        EntityWithoutVersion entity = EntityWithoutVersion.builder()
                .description("description")
                .build();
        return entityWithoutVersionRepository.save(entity);
    }

}
