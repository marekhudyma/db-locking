package com.marekhudyma.dbLockingPostgresql.locking;

import com.marekhudyma.dbLockingPostgresql.repository.EntityWithVersionOptimisticRepository;
import com.marekhudyma.dbLockingPostgresql.repository.EntityWithVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;


class LockingRepositoryIntegrationTest extends AbstractLockingRepositoryIntegrationTest {

    @Autowired
    private EntityWithVersionRepository entityWithVersionRepository;

    @Autowired
    private EntityWithVersionOptimisticRepository entityWithVersionOptimisticRepository;


//TODO HUDYMA FIX
//    // select ... where entitywith0_.id=? for share
//    // update entity_without_version set created=?, description=? where id=?
//    @Test
//    void shouldReadEntityWithPessimisticRead() throws Exception {
//        AtomicLong id = new AtomicLong();
//        executeInBlockingThread(() -> runInTransaction(() -> id.set(createEntityWithoutVersion().getId())));
//
//        RuntimeCountDownLatch t1Read = new RuntimeCountDownLatch(1);
//        RuntimeCountDownLatch t2Read = new RuntimeCountDownLatch(1);
//
//        ThreadWithException t1 = startThread(() -> runInTransaction(() -> {
//            EntityWithoutVersion entity = entityWithoutVersionPessimisticReadRepository.findOne(id.get());
//            t1Read.countDown();
//            t2Read.await();
//            entity.setDescription("description-changed-by-t1");
//            entityWithoutVersionRepository.save(entity);
//        }));
//
//        ThreadWithException t2 = startThread(() -> runInTransaction(() -> {
//            t1Read.await();
//            entityWithoutVersionPessimisticReadRepository.findOne(id.get());
//            t2Read.countDown();
//        }));
//
//        t1.join();
//        t2.join();
//
//        EntityWithoutVersion entity = entityWithoutVersionRepository.findOne(id.get());
//        assertThat(entity.getDescription()).isEqualTo("description-changed-by-t1");
//        assertThat(t1.getException()).isEqualTo(Optional.empty());
//        assertThat(t2.getException()).isEqualTo(Optional.empty());
//    }






}
