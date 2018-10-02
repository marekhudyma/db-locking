package com.marekhudyma.dbLockingPostgresql.repository;

import com.marekhudyma.dbLockingPostgresql.model.Account;
import com.marekhudyma.dbLockingPostgresql.utils.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountNativeRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AccountNativeRepository accountNativeRepository;

    @Autowired
    private AccountPessimisticWriteRepository accountPessimisticWriteRepository;

    @Test
    @Transactional
    void shouldFindAccount() throws Exception {
        UUID accountId = UUID.randomUUID();

        accountNativeRepository.insertOnConflictDoNothing(accountId);

        Account account = accountPessimisticWriteRepository.findById(accountId).get();

        assertThat(account.getId()).isEqualTo(accountId);
        assertThat(account.getVersion()).isEqualTo(0);
        assertThat(account.getCreated()).isNotNull();
    }

}
