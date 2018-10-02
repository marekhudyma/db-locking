package com.marekhudyma.dbLockingPostgresql.locking;

import com.marekhudyma.dbLockingPostgresql.model.EntityWithoutVersion;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;


class LockingPessimisticReadRepositoryIntegrationTest extends com.marekhudyma.dbLockingPostgresql.locking.AbstractLockingRepositoryIntegrationTest {

    // Hibernate: select entitywith0_.id as id1_2_0_, entitywith0_.created as created2_2_0_, entitywith0_.description as descript3_2_0_ from entity_without_version entitywith0_ where entitywith0_.id=? for share
    @Test
    @SuppressWarnings("Duplicates")
    void shouldReadEntityWithPessimisticRead() throws Exception {
        AtomicLong id = new AtomicLong();
        executeInBlockingThread(() -> runInTransaction(() -> id.set(createEntityWithoutVersion().getId())));

        RuntimeCountDownLatch t1Read = new RuntimeCountDownLatch(1);
        RuntimeCountDownLatch t2Read = new RuntimeCountDownLatch(1);

        ThreadWithException t1 = startThread(() -> runInTransaction(() -> {
            EntityWithoutVersion entity = entityWithoutVersionPessimisticReadRepository.findById(id.get()).get();
            t1Read.countDown();
            t2Read.await();
            entity.setDescription("description-changed-by-t1");
            entityWithoutVersionRepository.save(entity);
        }));

        ThreadWithException t2 = startThread(() -> runInTransaction(() -> {
            t1Read.await();
            entityWithoutVersionPessimisticReadRepository.findById(id.get());
            t2Read.countDown();
        }));

        t1.join();
        t2.join();

        EntityWithoutVersion entity = entityWithoutVersionRepository.findById(id.get()).get();
        assertThat(entity.getDescription()).isEqualTo("description-changed-by-t1");
        assertThat(t1.getException()).isEqualTo(Optional.empty());
        assertThat(t2.getException()).isEqualTo(Optional.empty());
    }

    @Test
    @SuppressWarnings("Duplicates")
    void shouldReadEntityWithPessimisticReadUsingNormalRepository() throws Exception {
        AtomicLong id = new AtomicLong();
        executeInBlockingThread(() -> runInTransaction(() -> id.set(createEntityWithoutVersion().getId())));

        RuntimeCountDownLatch t1Read = new RuntimeCountDownLatch(1);
        RuntimeCountDownLatch t2Read = new RuntimeCountDownLatch(1);

        AtomicLong t1ReadTimestamp = new AtomicLong();
        AtomicLong t2ReadTimestamp = new AtomicLong();

        ThreadWithException t1 = startThread(() -> runInTransaction(() -> {
            EntityWithoutVersion entity = entityWithoutVersionPessimisticReadRepository.findById(id.get()).get();
            t1Read.countDown();
            t2Read.await();
            entity.setDescription("description-changed-by-t1");
            entityWithoutVersionPessimisticReadRepository.save(entity);
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

//2017-11-30 23:14:20.757  WARN 622 --- [       Thread-9] o.h.e.j.s.SqlExceptionHelper             : SQL Error: 0, SQLState: 40P01
//2017-11-30 23:14:20.758 ERROR 622 --- [       Thread-9] o.h.e.j.s.SqlExceptionHelper             : ERROR: deadlock detected
//Detail: Process 76 waits for ShareLock on transaction 562; blocked by process 75.
//Process 75 waits for ExclusiveLock on tuple (0,1) of relation 16430 of database 16384; blocked by process 76.
//Hint: See server log for query details.
//Where: while updating tuple (0,1) in relation "entity_without_version"
    @Test
    @SuppressWarnings("Duplicates")
    void shouldKillOneRandomTransaction() throws Exception {
        AtomicLong id = new AtomicLong();
        executeInBlockingThread(() -> runInTransaction(() -> id.set(createEntityWithoutVersion().getId())));

        RuntimeCountDownLatch t1Read = new RuntimeCountDownLatch(1);
        RuntimeCountDownLatch t2Read = new RuntimeCountDownLatch(1);
        RuntimeCountDownLatch t1Write = new RuntimeCountDownLatch(1);

        ThreadWithException t1 = startThread(() -> runInTransaction(() -> {
            EntityWithoutVersion entity = entityWithoutVersionPessimisticReadRepository.findById(id.get()).get();
            t1Read.countDown();
            t2Read.await();
            entity.setDescription("description-changed-by-t1");
            entityWithoutVersionRepository.save(entity);
        }));

        ThreadWithException t2 = startThread(() -> runInTransaction(() -> {
            t1Read.await();
            EntityWithoutVersion entity = entityWithoutVersionPessimisticReadRepository.findById(id.get()).get();
            entity.setDescription("description-changed-by-t2");
            t2Read.countDown();
            t1Write.await();
            entityWithoutVersionPessimisticReadRepository.save(entity);
        }));

        t1Write.countDown();
        t1.join();
        t2.join();

        EntityWithoutVersion entity = entityWithoutVersionRepository.findById(id.get()).get();
        if(entity.getDescription().equals("description-changed-by-t1")) {
            assertThat(entity.getDescription()).isEqualTo("description-changed-by-t1");
            assertThat(t1.getException()).isEqualTo(Optional.empty());
            assertThat(t2.getException().get().getClass()).isEqualTo(CannotAcquireLockException.class);
            System.out.println("-------------------- T1 won");
        } else {
            assertThat(entity.getDescription()).isEqualTo("description-changed-by-t2");
            assertThat(t2.getException()).isEqualTo(Optional.empty());
            assertThat(t1.getException().get().getClass()).isEqualTo(CannotAcquireLockException.class);
            System.out.println("-------------------- T2 won");
        }
    }


    @Test
    @SuppressWarnings("Duplicates")
    void shouldOverrideStateOfT1BecauseUsingClassicalRepository() throws Exception {
        AtomicLong id = new AtomicLong();
        executeInBlockingThread(() -> runInTransaction(() -> id.set(createEntityWithoutVersion().getId())));

        RuntimeCountDownLatch t1Read = new RuntimeCountDownLatch(1);
        RuntimeCountDownLatch t2Read = new RuntimeCountDownLatch(1);
        RuntimeCountDownLatch t1Write = new RuntimeCountDownLatch(1);

        ThreadWithException t1 = startThread(() -> runInTransaction(() -> {
            EntityWithoutVersion entity = entityWithoutVersionPessimisticReadRepository.findById(id.get()).get();
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

        t1Write.countDown();
        t1.join();
        t2.join();

        EntityWithoutVersion entity = entityWithoutVersionRepository.findById(id.get()).get();

        assertThat(entity.getDescription()).isEqualTo("description-changed-by-t2");
        assertThat(t2.getException()).isEqualTo(Optional.empty());
        assertThat(t1.getException()).isEqualTo(Optional.empty());
        System.out.println("-------------------- T2 won");

    }

}
