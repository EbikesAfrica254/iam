package com.ebikes.iam.database.entities;

import com.ebikes.iam.database.entities.bases.BaseEntity;
import com.ebikes.iam.enums.TokenType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serial;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(
        name = "tokens",
        schema = "iam",
        indexes = {
                @Index(name = "idx_tokens_active_expiry", columnList = "user_extension_id,expires_at"),
                @Index(name = "idx_tokens_user_type", columnList = "user_extension_id,token_type")
        })
public class Token extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Builder.Default
    @Column(name = "consumed", nullable = false)
    private boolean consumed = false;

    @Column(name = "expires_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    @NotNull
    private OffsetDateTime expiresAt;

    @Column(name = "token_hash", nullable = false, unique = true)
    @NotBlank
    @Size(min = 64, max = 64)
    private String tokenHash;

    @Column(name = "token_type", columnDefinition = "iam.token_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @NotNull
    private TokenType tokenType;

    @Column(name = "user_extension_id", nullable = false)
    @NotNull
    private UUID userExtensionId;

    @Version
    private Long version;

    public void consume() {
        this.consumed = true;
    }

    public boolean isExpired(OffsetDateTime currentTime) {
        return currentTime.isAfter(expiresAt);
    }
}
