package com.marekhudyma.dbLockingPostgresql.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.springframework.data.domain.Persistable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "operations")
@Getter
@Setter
@EqualsAndHashCode(exclude = {"created"})
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString
public class Operation implements Persistable<UUID> {

    @Id
    private UUID id;

    @Generated(value = GenerationTime.INSERT)
    private Instant created;

    @NonNull
    String description;

    @NonNull
    private UUID accountId;

    @Version
    private Integer version;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

}
