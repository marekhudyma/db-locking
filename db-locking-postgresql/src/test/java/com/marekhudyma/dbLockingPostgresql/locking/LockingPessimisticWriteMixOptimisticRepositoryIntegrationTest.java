package com.marekhudyma.dbLockingPostgresql.locking;

import com.marekhudyma.dbLockingPostgresql.model.EntityWithVersion;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;


class LockingPessimisticWriteMixOptimisticRepositoryIntegrationTest extends com.marekhudyma.dbLockingPostgresql.locking.AbstractLockingRepositoryIntegrationTest {


    @Test
    @SuppressWarnings("Duplicates")
    void shouldNotReadEntityWithPessimisticWrite() throws Exception {
        AtomicReference<EntityWithVersion> atomicReference = new AtomicReference<>();
        executeInBlockingThread(() -> runInTransaction(() -> atomicReference.set(createEntityWithVersion())));

        ThreadWithException t1 =executeInBlockingThread(() -> runInTransaction(() -> {
            EntityWithVersion entity = entityWithVersionPessimisticWriteRepository
                    .findById(atomicReference.get().getId()).get();
            entity.setDescription("description-changed-by-t1");
            entityWithVersionRepository.save(entity);
        }));

        ThreadWithException t2 = executeInBlockingThread(() -> runInTransaction(() -> {
            entityWithVersionPessimisticWriteRepository.save(atomicReference.get());
        }));

        t2.join();

        EntityWithVersion entity = entityWithVersionRepository.findById(atomicReference.get().getId()).get();
        assertThat(entity.getDescription()).isEqualTo("description-changed-by-t1");
        assertThat(t1.getException()).isEqualTo(Optional.empty());
        assertThat(t2.getException().get().getClass()).isEqualTo(ObjectOptimisticLockingFailureException.class);
    }



}
