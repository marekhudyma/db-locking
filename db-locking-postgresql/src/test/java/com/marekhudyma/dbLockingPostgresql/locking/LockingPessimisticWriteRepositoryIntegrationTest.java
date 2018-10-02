package com.marekhudyma.dbLockingPostgresql.locking;

import com.marekhudyma.dbLockingPostgresql.model.EntityWithoutVersion;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class LockingPessimisticWriteRepositoryIntegrationTest extends com.marekhudyma.dbLockingPostgresql.locking.AbstractLockingRepositoryIntegrationTest {

    // Hibernate: select entitywith0_.id as id1_2_0_, entitywith0_.created as created2_2_0_, entitywith0_.description as descript3_2_0_ from entity_without_version entitywith0_ where entitywith0_.id=? for update

    @Test
    @SuppressWarnings("Duplicates")
    void shouldNotReadEntityWithPessimisticWrite() throws Exception {
        AtomicLong id = new AtomicLong();
        executeInBlockingThread(() -> runInTransaction(() -> id.set(createEntityWithoutVersion().getId())));

        RuntimeCountDownLatch t1Read = new RuntimeCountDownLatch(1);

        AtomicLong t1ReadTimestamp = new AtomicLong();
        AtomicLong t2ReadTimestamp = new AtomicLong();

        ThreadWithException t1 = startThread(() -> runInTransaction(() -> {
            EntityWithoutVersion entity = entityWithoutVersionPessimisticWriteRepository.findById(id.get()).get();
            t1Read.countDown();
            entity.setDescription("description-changed-by-t1");
            entityWithoutVersionRepository.save(entity);
            t1ReadTimestamp.set(System.currentTimeMillis());
        }));

        ThreadWithException t2 = startThread(() -> runInTransaction(() -> {
            t1Read.await();
            entityWithoutVersionPessimisticWriteRepository.findById(id.get());
            t2ReadTimestamp.set(System.currentTimeMillis());

        }));

        t1.join();
        t2.join();

        EntityWithoutVersion entity = entityWithoutVersionRepository.findById(id.get()).get();
        assertThat(entity.getDescription()).isEqualTo("description-changed-by-t1");
        assertThat(t1ReadTimestamp.get()).isLessThan(t2ReadTimestamp.get());
        assertThat(t1.getException()).isEqualTo(Optional.empty());
        assertThat(t2.getException()).isEqualTo(Optional.empty());
    }

    @Test
    @SuppressWarnings("Duplicates")
    void shouldReadEntityWithPessimisticWriteUsingNormalRepository() throws Exception {
        AtomicLong id = new AtomicLong();
        executeInBlockingThread(() -> runInTransaction(() -> id.set(createEntityWithoutVersion().getId())));

        RuntimeCountDownLatch t1Read = new RuntimeCountDownLatch(1);
        RuntimeCountDownLatch t2Read = new RuntimeCountDownLatch(1);

        AtomicLong t1ReadTimestamp = new AtomicLong();
        AtomicLong t2ReadTimestamp = new AtomicLong();

        ThreadWithException t1 = startThread(() -> runInTransaction(() -> {
            EntityWithoutVersion entity = entityWithoutVersionPessimisticWriteRepository.findById(id.get()).get();
            t1Read.countDown();
            t2Read.await();
            entity.setDescription("description-changed-by-t1");
            entityWithoutVersionPessimisticWriteRepository.save(entity);
            t1ReadTimestamp.set(System.currentTimeMillis());
        }));

        ThreadWithException t2 = startThread(() -> runInTransaction(() -> {
            t1Read.await();
            entityWithoutVersionRepository.findById(id.get()).get();
            t2ReadTimestamp.set(System.currentTimeMillis());
            t2Read.countDown();
        }));

        t1.join();
        t2.join();

        EntityWithoutVersion entity = entityWithoutVersionRepository.findById(id.get()).get();
        assertThat(entity.getDescription()).isEqualTo("description-changed-by-t1");
        assertThat(t2ReadTimestamp.get()).isLessThanOrEqualTo(t1ReadTimestamp.get());
        assertThat(t1.getException()).isEqualTo(Optional.empty());
        assertThat(t2.getException()).isEqualTo(Optional.empty());
    }

}
