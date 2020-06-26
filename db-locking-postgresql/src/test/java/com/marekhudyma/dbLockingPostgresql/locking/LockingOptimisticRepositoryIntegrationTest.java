package com.marekhudyma.dbLockingPostgresql.locking;

import com.marekhudyma.dbLockingPostgresql.model.EntityWithVersion;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import javax.transaction.Transactional;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;


class LockingOptimisticRepositoryIntegrationTest extends com.marekhudyma.dbLockingPostgresql.locking.AbstractLockingRepositoryIntegrationTest {

    @Test
    @SuppressWarnings("Duplicates")
    void shouldUpdateVersionAtTheEndOfTransactionForOptimistic() throws Exception {
        AtomicLong id = new AtomicLong();
        executeInBlockingThread(() -> runInTransaction(() -> {
            EntityWithVersion entity = EntityWithVersion.builder()
                    .description("description")
                    .build();
            entityWithVersionOptimisticRepository.save(entity);

            entity = entityWithVersionOptimisticRepository.findById(entity.getId()).get();
            entity.setDescription("description-changed");
            entity = entityWithVersionOptimisticRepository.save(entity);

            entity = entityWithVersionOptimisticRepository.findById(entity.getId()).get();
            entity.setDescription("description-changed-2");
            entityWithVersionOptimisticRepository.save(entity);

            id.set(entity.getId());
        }));

        executeInBlockingThread(() -> runInTransaction(() -> {
            EntityWithVersion entity = entityWithVersionOptimisticRepository.findById(id.get()).get();
            entity.setDescription("description-changed-3");
            entity = entityWithVersionOptimisticRepository.save(entity);

            entity = entityWithVersionOptimisticRepository.findById(entity.getId()).get();
            entity.setDescription("description-changed-4");
            entityWithVersionOptimisticRepository.save(entity);
        }));

        executeInBlockingThread(() -> runInTransaction(() -> {
            EntityWithVersion entity = entityWithVersionOptimisticRepository.findById(id.get()).get();
            entity.setDescription("description-changed-5");
            entity = entityWithVersionOptimisticRepository.save(entity);

            entity = entityWithVersionOptimisticRepository.findById(entity.getId()).get();
            entity.setDescription("description-changed-6");
            entityWithVersionOptimisticRepository.save(entity);
        }));

        EntityWithVersion actual = entityWithVersionOptimisticRepository.findById(id.get()).get();
        assertThat(actual.getVersion()).isEqualTo(3);
    }

    @Test
    @SuppressWarnings("Duplicates")
    void shouldUpdateVersionEveryTimeForSaveAndFlushForOptimistic() throws Exception {
        AtomicLong id = new AtomicLong();
        executeInBlockingThread(() -> runInTransaction(() -> {
            EntityWithVersion entity = EntityWithVersion.builder()
                    .description("description")
                    .build();
            entityWithVersionOptimisticRepository.saveAndFlush(entity);

            entity = entityWithVersionOptimisticRepository.findById(entity.getId()).get();
            entity.setDescription("description-changed-1");
            entity = entityWithVersionOptimisticRepository.saveAndFlush(entity);

            entity = entityWithVersionOptimisticRepository.findById(entity.getId()).get();
            entity.setDescription("description-changed-2");
            entityWithVersionOptimisticRepository.saveAndFlush(entity);

            id.set(entity.getId());
        }));

        EntityWithVersion actual = entityWithVersionOptimisticRepository.findById(id.get()).get();
        assertThat(actual.getVersion()).isEqualTo(2);
    }

    @Test
    @SuppressWarnings("Duplicates")
    @Transactional
    void shouldUpdateVersionEveryTimeForSaveForOptimisticForceIncrement() throws Exception {
        AtomicLong id = new AtomicLong();
        executeInBlockingThread(() -> runInTransaction(() -> {
            EntityWithVersion entity = EntityWithVersion.builder()
                    .description("description")
                    .build();
            entityWithVersionOptimisticForceIncrementRepository.saveAndFlush(entity);

            entity = entityWithVersionOptimisticForceIncrementRepository.findById(entity.getId()).get();
            entity.setDescription("description-changed-1");
            entity = entityWithVersionOptimisticForceIncrementRepository.saveAndFlush(entity);

            entity = entityWithVersionOptimisticForceIncrementRepository.findById(entity.getId()).get();
            entity.setDescription("description-changed-2");
            entityWithVersionOptimisticForceIncrementRepository.saveAndFlush(entity);

            id.set(entity.getId());
        }));

        EntityWithVersion afterT1 = entityWithVersionOptimisticForceIncrementRepository.findById(id.get()).get();
        assertThat(afterT1.getVersion()).isEqualTo(2);

        executeInBlockingThread(() -> runInTransaction(() -> {
            EntityWithVersion entity = entityWithVersionOptimisticForceIncrementRepository.findById(id.get()).get();
            entity.setDescription("description-changed-3");
            entityWithVersionOptimisticForceIncrementRepository.saveAndFlush(entity);

            entity = entityWithVersionOptimisticForceIncrementRepository.findById(entity.getId()).get();
            entity.setDescription("description-changed-4");
            entityWithVersionOptimisticForceIncrementRepository.saveAndFlush(entity);

            entity = entityWithVersionOptimisticForceIncrementRepository.findById(entity.getId()).get();
            entity.setDescription("description-changed-5");
            entityWithVersionOptimisticForceIncrementRepository.saveAndFlush(entity);
        }));

        EntityWithVersion afterT2 = entityWithVersionOptimisticForceIncrementRepository.findById(id.get()).get();
        assertThat(afterT2.getVersion()).isEqualTo(2);

        executeInBlockingThread(() -> runInTransaction(() -> {
            EntityWithVersion entity = entityWithVersionOptimisticForceIncrementRepository.findById(id.get()).get();
            entity.setDescription("description-changed-6");
            entity = entityWithVersionOptimisticForceIncrementRepository.saveAndFlush(entity);

            entity = entityWithVersionOptimisticForceIncrementRepository.findById(entity.getId()).get();
            entity.setDescription("description-changed-7");
            entityWithVersionOptimisticForceIncrementRepository.saveAndFlush(entity);
        }));

        EntityWithVersion afterT3 = entityWithVersionOptimisticForceIncrementRepository.findById(id.get()).get();
        assertThat(afterT3.getVersion()).isEqualTo(2);
    }

    // select entitywith0_.id ... from entity_with_version entitywith0_ where entitywith0_.id=?
    // update entity_with_version set created=?, description=?, version=? where id=? and version=?
    // this tests also show that the same result we have when we use
    // @Lock(LockModeType.OPTIMISTIC)
    // Optional<EntityWithVersion> findById(Long id);
    // and without @Lock annotation.
    @Test
    @SuppressWarnings("Duplicates")
    void shouldDoOptimisticLockingForEntityWithVersionDefaultfindById() throws Exception {
        AtomicLong id = new AtomicLong();
        executeInBlockingThread(() -> runInTransaction(() -> id.set(createEntityWithVersion().getId())));

        RuntimeCountDownLatch t1Read = new RuntimeCountDownLatch(1);
        RuntimeCountDownLatch t2ReadWrite = new RuntimeCountDownLatch(1);

        ThreadWithException t1 = startThread(() -> runInTransaction(() -> {
            EntityWithVersion entity = entityWithVersionOptimisticRepository.findById(id.get()).get();
            t1Read.countDown();
            t2ReadWrite.await();
            entity.setDescription("description-changed-by-t1");
            entityWithVersionOptimisticRepository.save(entity);
        }));

        ThreadWithException t2 = startThread(() -> runInTransaction(() -> {
            t1Read.await();
            EntityWithVersion entity = entityWithVersionOptimisticRepository.findById(id.get()).get();
            entity.setDescription("description-changed-by-t2");
            System.out.println("------------------------------------------------------------------------------1");
            entityWithVersionOptimisticRepository.save(entity);
            System.out.println("------------------------------------------------------------------------------2");
        }));

        t2.join();
        t2ReadWrite.countDown();
        t1.join();

        EntityWithVersion entity = entityWithVersionOptimisticRepository.findById(id.get()).get();

        assertThat(entity.getDescription()).isEqualTo("description-changed-by-t2");
        assertThat(t1.getException().get().getClass()).isEqualTo(ObjectOptimisticLockingFailureException.class);
        assertThat(t2.getException()).isEqualTo(Optional.empty());
    }

}
