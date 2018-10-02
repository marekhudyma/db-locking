package com.marekhudyma.dbLockingPostgresql.repository;

import com.marekhudyma.dbLockingPostgresql.model.EntityWithVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface EntityWithVersionPessimisticReadRepository extends JpaRepository<EntityWithVersion, Long> {

    @Override
    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<EntityWithVersion> findById(Long id);
}
