package com.marekhudyma.dbLockingPostgresql.repository;

import com.marekhudyma.dbLockingPostgresql.model.EntityWithVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface EntityWithVersionRepository extends JpaRepository<EntityWithVersion, Long> {

}
