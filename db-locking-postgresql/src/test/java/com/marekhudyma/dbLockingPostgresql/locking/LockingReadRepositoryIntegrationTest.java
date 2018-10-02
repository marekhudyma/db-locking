package com.marekhudyma.dbLockingPostgresql.locking;

import com.marekhudyma.dbLockingPostgresql.model.EntityWithVersion;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;


class LockingReadRepositoryIntegrationTest extends AbstractLockingRepositoryIntegrationTest {

    @Test
    @SuppressWarnings("Duplicates")
    void shouldUpdateVersionAtTheEndOfTransactionForOptimistic() throws Exception {
        AtomicLong id = new AtomicLong();
        executeInBlockingThread(() -> runInTransaction(() -> {
            EntityWithVersion entity = EntityWithVersion.builder()
                    .description("description")
                    .build();
            entityWithVersionReadRepository.save(entity);

            entity = entityWithVersionReadRepository.findById(entity.getId()).get();
            entity.setDescription("description-changed");
            entity = entityWithVersionReadRepository.save(entity);

            entity = entityWithVersionReadRepository.findById(entity.getId()).get();
            entity.setDescription("description-changed-2");
            entityWithVersionReadRepository.save(entity);

            id.set(entity.getId());
        }));

        executeInBlockingThread(() -> runInTransaction(() -> {
            EntityWithVersion entity = entityWithVersionReadRepository.findById(id.get()).get();
            entity.setDescription("description-changed-3");
            entity = entityWithVersionReadRepository.save(entity);

            entity = entityWithVersionReadRepository.findById(entity.getId()).get();
            entity.setDescription("description-changed-4");
            entityWithVersionReadRepository.save(entity);
        }));

        executeInBlockingThread(() -> runInTransaction(() -> {
            EntityWithVersion entity = entityWithVersionReadRepository.findById(id.get()).get();
            entity.setDescription("description-changed-5");
            entity = entityWithVersionReadRepository.save(entity);

            entity = entityWithVersionReadRepository.findById(entity.getId()).get();
            entity.setDescription("description-changed-6");
            entityWithVersionReadRepository.save(entity);
        }));

        EntityWithVersion actual = entityWithVersionReadRepository.findById(id.get()).get();
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
            entityWithVersionReadRepository.saveAndFlush(entity);

            entity = entityWithVersionReadRepository.findById(entity.getId()).get();
            entity.setDescription("description-changed-1");
            entity = entityWithVersionReadRepository.saveAndFlush(entity);

            entity = entityWithVersionReadRepository.findById(entity.getId()).get();
            entity.setDescription("description-changed-2");
            entityWithVersionReadRepository.saveAndFlush(entity);

            id.set(entity.getId());
        }));

        EntityWithVersion actual = entityWithVersionReadRepository.findById(id.get()).get();
        assertThat(actual.getVersion()).isEqualTo(2);
    }


    @Test
    @SuppressWarnings("Duplicates")
    void shouldUpdateVersionAtTheEndOfTransactionForRead() throws Exception {
        AtomicLong id = new AtomicLong();
        executeInBlockingThread(() -> runInTransaction(() -> {
            EntityWithVersion entity = EntityWithVersion.builder()
                    .description("description")
                    .build();
            entityWithVersionReadRepository.save(entity);

            entity = entityWithVersionReadRepository.findById(entity.getId()).get();
            entity.setDescription("description-changed");
            entity = entityWithVersionReadRepository.save(entity);

            entity = entityWithVersionReadRepository.findById(entity.getId()).get();
            entity.setDescription("description-changed-2");
            entityWithVersionReadRepository.save(entity);

            id.set(entity.getId());
        }));

        executeInBlockingThread(() -> runInTransaction(() -> {
            EntityWithVersion entity = entityWithVersionReadRepository.findById(id.get()).get();
            entity.setDescription("description-changed-3");
            entity = entityWithVersionReadRepository.save(entity);

            entity = entityWithVersionReadRepository.findById(entity.getId()).get();
            entity.setDescription("description-changed-4");
            entityWithVersionReadRepository.save(entity);
        }));

        executeInBlockingThread(() -> runInTransaction(() -> {
            EntityWithVersion entity = entityWithVersionReadRepository.findById(id.get()).get();
            entity.setDescription("description-changed-5");
            entity = entityWithVersionReadRepository.save(entity);

            entity = entityWithVersionReadRepository.findById(entity.getId()).get();
            entity.setDescription("description-changed-6");
            entityWithVersionReadRepository.save(entity);
        }));

        EntityWithVersion actual = entityWithVersionReadRepository.findById(id.get()).get();
        assertThat(actual.getVersion()).isEqualTo(3);
    }

    // select entitywith0_.id ... from entity_with_version entitywith0_ where entitywith0_.id=?
    // update entity_with_version set created=?, description=?, version=? where id=? and version=?
    @Test
    @SuppressWarnings("Duplicates")
    void shouldDoOptimisticLockingForEntityWithVersionDefaultFindById() throws Exception {
        AtomicLong id = new AtomicLong();
        executeInBlockingThread(() -> runInTransaction(() -> id.set(createEntityWithVersion().getId())));

        RuntimeCountDownLatch t1Read = new RuntimeCountDownLatch(1);
        RuntimeCountDownLatch t2ReadWrite = new RuntimeCountDownLatch(1);

        ThreadWithException t1 = startThread(() -> runInTransaction(() -> {
            EntityWithVersion entity = entityWithVersionReadRepository.findById(id.get()).get();
            t1Read.countDown();
            t2ReadWrite.await();
            entity.setDescription("description-changed-by-t1");
            entityWithVersionReadRepository.save(entity);
        }));

        ThreadWithException t2 = startThread(() -> runInTransaction(() -> {
            t1Read.await();
            EntityWithVersion entity = entityWithVersionReadRepository.findById(id.get()).get();
            entity.setDescription("description-changed-by-t2");
            entityWithVersionReadRepository.save(entity);
        }));

        t2.join();
        t2ReadWrite.countDown();
        t1.join();

        EntityWithVersion entity = entityWithVersionReadRepository.findById(id.get()).get();

        assertThat(entity.getDescription()).isEqualTo("description-changed-by-t2");
        assertThat(t1.getException().get().getClass()).isEqualTo(ObjectOptimisticLockingFailureException.class);
        assertThat(t2.getException()).isEqualTo(Optional.empty());
    }
}
