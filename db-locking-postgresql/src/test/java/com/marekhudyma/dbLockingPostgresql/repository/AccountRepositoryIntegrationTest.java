package com.marekhudyma.dbLockingPostgresql.repository;


import com.marekhudyma.dbLockingPostgresql.locking.AbstractLockingRepositoryIntegrationTest;
import com.marekhudyma.dbLockingPostgresql.model.Account;
import com.marekhudyma.dbLockingPostgresql.model.EntityWithVersion;
import com.marekhudyma.dbLockingPostgresql.utils.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


class AccountRepositoryIntegrationTest extends AbstractLockingRepositoryIntegrationTest {

    @Autowired
    private AccountPessimisticWriteRepository accountRepository;

    @Test
    @Transactional
    void shouldFindAccount() throws Exception {
        Account account = accountRepository.saveAndFlush(Account.builder().id(UUID.randomUUID()).build());

        Account actual = accountRepository.findById(account.getId()).get();

        assertThat(actual).isEqualTo(account);
    }

    @Test
    @Transactional
    void shouldModifyAccount() throws Exception {
        UUID id = UUID.randomUUID();


        executeInBlockingThread(() -> runInTransaction(() -> {
            accountRepository.saveAndFlush(Account.builder().id(id).build());
        }));

        executeInBlockingThread(() -> runInTransaction(() -> {
            Account account = accountRepository.findById(id).get();
            account.setName("Marek");
            accountRepository.saveAndFlush(account);
        }));

        Account actual = accountRepository.findAll().stream()
                .filter(a -> Objects.equals(a.getId(), id))
                .findFirst()
                .get();
        Account expected = Account.builder().id(id).name("Marek").build();

        assertThat(actual).isEqualToIgnoringGivenFields(expected, "created", "version");
        assertThat(actual.getVersion()).isEqualTo(1);
    }


}

