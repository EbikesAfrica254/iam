package com.ebikes.iam.database.entities.bases;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Getter
@MappedSuperclass
@NoArgsConstructor
@SuperBuilder
public abstract class SoftDeletableEntity extends BaseEntity {

    @Column(name = "deleted_at", columnDefinition = "TIMESTAMPTZ")
    protected OffsetDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        if (isDeleted()) {
            throw new IllegalStateException(
                    String.format(
                            "Entity %s (id=%s) is already deleted", getClass().getSimpleName(), getId()));
        }
        this.deletedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
