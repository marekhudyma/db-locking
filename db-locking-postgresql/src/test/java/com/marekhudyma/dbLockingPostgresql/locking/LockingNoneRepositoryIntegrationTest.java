package com.marekhudyma.dbLockingPostgresql.locking;

import com.marekhudyma.dbLockingPostgresql.model.EntityWithoutVersion;
import com.marekhudyma.dbLockingPostgresql.repository.EntityWithoutVersionNoneRepository;
import com.marekhudyma.dbLockingPostgresql.repository.EntityWithoutVersionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;


class LockingNoneRepositoryIntegrationTest extends com.marekhudyma.dbLockingPostgresql.locking.AbstractLockingRepositoryIntegrationTest {

    @Autowired
    private EntityWithoutVersionRepository entityWithoutVersionRepository;

    @Autowired
    private EntityWithoutVersionNoneRepository entityWithoutVersionNoneRepository;

    // select entitywith_.created as created2_2_ from entity_without_version entitywith_ where entitywith_.id=?
    // update entity_without_version set created=?, description=? where id=?
    @Test
    @SuppressWarnings("Duplicates")
    void shouldDoNotLockingForEntityWithoutVersionDefaultFindOne() throws Exception {
        AtomicLong id = new AtomicLong();
        executeInBlockingThread(() -> runInTransaction(() -> id.set(createEntityWithoutVersion().getId())));

        RuntimeCountDownLatch t1Read = new RuntimeCountDownLatch(1);
        RuntimeCountDownLatch t2Read = new RuntimeCountDownLatch(1);
        RuntimeCountDownLatch t1Write = new RuntimeCountDownLatch(1);

        ThreadWithException t1 = startThread(() -> runInTransaction(() -> {
            EntityWithoutVersion entity = entityWithoutVersionRepository.findById(id.get()).get();
            t1Read.countDown();
            t2Read.await();
            entity.setDescription("description-changed-by-t1");
            entityWithoutVersionRepository.save(entity);
        }));

        ThreadWithException t2 = startThread(() -> runInTransaction(() -> {
            t1Read.await();
            EntityWithoutVersion entity = entityWithoutVersionRepository.findById(id.get()).get();
            entity.setDescription("description-changed-by-t2");
            t2Read.countDown();
            t1Write.await();
            entityWithoutVersionRepository.save(entity);
        }));

        t1.join();
        t1Write.countDown();

        t2.join();

        EntityWithoutVersion entity = entityWithoutVersionRepository.findById(id.get()).get();

        assertThat(entity.getDescription()).isEqualTo("description-changed-by-t2");
        assertThat(t1.getException()).isEqualTo(Optional.empty());
        assertThat(t2.getException()).isEqualTo(Optional.empty());
    }

    // select entitywith_.created as created2_2_ from entity_without_version entitywith_ where entitywith_.id=?
    // update entity_without_version set created=?, description=? where id=?
    @Test
    @SuppressWarnings("Duplicates")
    void shouldDoNotLockingForEntityWithoutVersionLockingNoneFindOne() throws Exception {
        AtomicLong id = new AtomicLong();
        executeInBlockingThread(() -> runInTransaction(() -> id.set(createEntityWithoutVersion().getId())));

        RuntimeCountDownLatch t1Read = new RuntimeCountDownLatch(1);
        RuntimeCountDownLatch t2Read = new RuntimeCountDownLatch(1);
        RuntimeCountDownLatch t1Write = new RuntimeCountDownLatch(1);

        ThreadWithException t1 = startThread(() -> runInTransaction(() -> {
            EntityWithoutVersion entity = entityWithoutVersionNoneRepository.findById(id.get()).get();
            t1Read.countDown();
            t2Read.await();
            entity.setDescription("description-changed-by-t1");
            entityWithoutVersionNoneRepository.save(entity);
        }));

        ThreadWithException t2 = startThread(() -> runInTransaction(() -> {
            t1Read.await();
            EntityWithoutVersion entity = entityWithoutVersionNoneRepository.findById(id.get()).get();
            t2Read.countDown();
            t1Write.await();
            entity.setDescription("description-changed-by-t2");
            entityWithoutVersionNoneRepository.save(entity);
        }));

        t1.join();
        t1Write.countDown();

        t2.join();

        EntityWithoutVersion entity = entityWithoutVersionNoneRepository.findById(id.get()).get();

        assertThat(entity.getDescription()).isEqualTo("description-changed-by-t2");
        assertThat(t1.getException()).isEqualTo(Optional.empty());
        assertThat(t2.getException()).isEqualTo(Optional.empty());
    }

}
