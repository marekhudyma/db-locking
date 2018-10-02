package com.marekhudyma.dbLockingPostgresql.locking;

import com.marekhudyma.dbLockingPostgresql.model.EntityWithVersion;
import com.marekhudyma.dbLockingPostgresql.model.EntityWithoutVersion;
import com.marekhudyma.dbLockingPostgresql.repository.EntityWithVersionOptimisticForceIncrementRepository;
import com.marekhudyma.dbLockingPostgresql.repository.EntityWithVersionOptimisticRepository;
import com.marekhudyma.dbLockingPostgresql.repository.EntityWithVersionPessimisticReadRepository;
import com.marekhudyma.dbLockingPostgresql.repository.EntityWithVersionPessimisticWriteForceIncrementRepository;
import com.marekhudyma.dbLockingPostgresql.repository.EntityWithVersionPessimisticWriteRepository;
import com.marekhudyma.dbLockingPostgresql.repository.EntityWithVersionReadRepository;
import com.marekhudyma.dbLockingPostgresql.repository.EntityWithVersionRepository;
import com.marekhudyma.dbLockingPostgresql.repository.EntityWithoutVersionPessimisticReadRepository;
import com.marekhudyma.dbLockingPostgresql.repository.EntityWithoutVersionPessimisticWriteRepository;
import com.marekhudyma.dbLockingPostgresql.repository.EntityWithoutVersionRepository;
import com.marekhudyma.dbLockingPostgresql.utils.AbstractIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

public abstract class AbstractLockingRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    protected EntityWithVersionRepository entityWithVersionRepository;

    @Autowired
    protected EntityWithoutVersionRepository entityWithoutVersionRepository;

    @Autowired
    protected EntityWithVersionPessimisticReadRepository entityWithVersionPessimisticReadRepository;

    @Autowired
    protected EntityWithoutVersionPessimisticReadRepository entityWithoutVersionPessimisticReadRepository;

    @Autowired
    protected EntityWithoutVersionPessimisticWriteRepository entityWithoutVersionPessimisticWriteRepository;

    @Autowired
    protected EntityWithVersionOptimisticRepository entityWithVersionOptimisticRepository;

    @Autowired
    protected EntityWithVersionReadRepository entityWithVersionReadRepository;

    @Autowired
    protected EntityWithVersionOptimisticForceIncrementRepository entityWithVersionOptimisticForceIncrementRepository;

    @Autowired
    protected EntityWithVersionPessimisticWriteForceIncrementRepository
            entityWithVersionPessimisticWriteForceIncrementRepository;

    @Autowired
    protected EntityWithVersionPessimisticWriteRepository entityWithVersionPessimisticWriteRepository;

    @Autowired
    protected TransactionTemplate transactionTemplate;

    protected ThreadWithException startThread(Runnable runnable) {
        ThreadWithException t = new ThreadWithException(runnable);
        t.start();
        return t;
    }

    protected ThreadWithException executeInBlockingThread(Runnable runnable) {
        ThreadWithException t = startThread(runnable);
        try {
            t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return t;
    }

    protected void runInTransaction(Runnable runnable) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                runnable.run();
            }
        });
    }

    protected static class ThreadWithException extends Thread {

        private final Runnable runnable;

        private Optional<Exception> exception;

        public ThreadWithException(Runnable runnable) {
            this.runnable = runnable;
            this.exception = Optional.empty();
        }

        @Override
        public void run() {
            try {
                this.runnable.run();
            } catch (Exception e) {
                this.exception = Optional.of(e);
            }
        }

        public Optional<Exception> getException() {
            return exception;
        }
    }

    protected static class RuntimeCountDownLatch extends CountDownLatch {

        public RuntimeCountDownLatch(int count) {
            super(count);
        }

        @Override
        public void await() {
            try {
                super.await();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected EntityWithVersion createEntityWithVersion() {
        EntityWithVersion entity = EntityWithVersion.builder()
                .description("description")
                .build();
        return entityWithVersionRepository.save(entity);
    }

    protected EntityWithoutVersion createEntityWithoutVersion() {
        EntityWithoutVersion entity = EntityWithoutVersion.builder()
                .description("description")
                .build();
        return entityWithoutVersionRepository.save(entity);
    }
}
