package com.marekhudyma.dbLockingPostgresql.repository;

import com.marekhudyma.dbLockingPostgresql.model.Account;
import com.marekhudyma.dbLockingPostgresql.model.Operation;
import com.marekhudyma.dbLockingPostgresql.utils.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


class OperationRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AccountPessimisticWriteRepository accountRepository;

    @Autowired
    private OperationRepository operationRepository;

    @Test
    void shouldSaveAndFindOperation() throws Exception {
        UUID accountId = UUID.randomUUID();
        Account account = createAccount(accountId);

        Operation operation = Operation.builder()
                .id(UUID.randomUUID())
                .description("description")
                .accountId(account.getId())
                .build();
        operation = operationRepository.save(operation);

        Operation actual = operationRepository.findById(operation.getId()).get();

        assertThat(actual).isEqualTo(operation);
    }

    private Account createAccount(UUID id) {
        Account account = Account.builder()
                .id(id)
                .build();
        return accountRepository.save(account);
    }

}
