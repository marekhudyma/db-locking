package com.marekhudyma.dbLockingPostgresql.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static java.lang.String.format;

@Service
@RequiredArgsConstructor
public class AccountNativeRepository {

    private final JdbcTemplate template;

    @Transactional
    public void insertOnConflictDoNothing(UUID accountId) {
        String sql = format("INSERT INTO Accounts (id, version) " +
                        "VALUES('%s', %d) ON CONFLICT (id) DO NOTHING",
                accountId.toString(),
                0);
        template.execute(sql);
    }

}


