package com.marekhudyma.dbLockingPostgresql.repository;
import com.marekhudyma.dbLockingPostgresql.model.EntityWithoutVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface EntityWithoutVersionPessimisticWriteRepository extends JpaRepository<EntityWithoutVersion, Long> {

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EntityWithoutVersion> findById(Long id);
}
