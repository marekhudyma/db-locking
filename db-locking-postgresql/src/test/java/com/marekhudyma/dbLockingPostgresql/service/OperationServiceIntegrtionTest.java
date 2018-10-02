package com.marekhudyma.dbLockingPostgresql.service;

import com.google.common.collect.ImmutableList;
import com.marekhudyma.dbLockingPostgresql.locking.AbstractLockingRepositoryIntegrationTest;
import com.marekhudyma.dbLockingPostgresql.model.Account;
import com.marekhudyma.dbLockingPostgresql.model.Operation;
import com.marekhudyma.dbLockingPostgresql.repository.AccountPessimisticWriteRepository;
import com.marekhudyma.dbLockingPostgresql.repository.OperationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


class OperationServiceIntegrtionTest extends AbstractLockingRepositoryIntegrationTest {

    @Autowired
    protected com.marekhudyma.dbLockingPostgresql.service.OperationService operationService;

    @Autowired
    protected AccountPessimisticWriteRepository accountPessimisticWriteRepository;

    @Autowired
    protected OperationRepository operationRepository;

    @Test
    @Transactional
    @SuppressWarnings("Duplicates")
    void shouldCreateOperations() throws Exception {

        RuntimeCountDownLatch countDown = new RuntimeCountDownLatch(1);
        UUID accountId = UUID.randomUUID();
        List<Operation> operations1 = ImmutableList.of(Operation.builder()
                .id(UUID.randomUUID())
                .description("description-1")
                .accountId(accountId)
                .build());
        List<Operation> operations2 = ImmutableList.of(Operation.builder()
                .id(UUID.randomUUID())
                .description("description-2")
                .accountId(accountId)
                .build());

        ThreadWithException t1 = startThread(() -> runInTransaction(() -> {
            operationService.createOperations(accountId, operations1);
            countDown.countDown();
        }));

        ThreadWithException t2 = startThread(() -> runInTransaction(() -> {
            countDown.await();
            operationService.createOperations(accountId, operations2);
        }));

        t1.join();
        t2.join();

        Account account = accountPessimisticWriteRepository.findById(accountId).get();
        assertThat(account).isNotNull();

        Operation operation1 = operationRepository.findById(operations1.get(0).getId()).get();
        Operation operation2 = operationRepository.findById(operations2.get(0).getId()).get();

        assertThat(operation1.getCreated()).isBeforeOrEqualTo(operation2.getCreated());
    }

}
