package com.ebikes.iam.database.entities;

import com.ebikes.iam.database.entities.bases.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(
        name = "memberships",
        schema = "iam",
        indexes = {
                @Index(name = "idx_membership_keycloak_id", columnList = "keycloak_user_id"),
                @Index(name = "idx_membership_user_extension_id", columnList = "user_extension_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_memberships_user_org",
                        columnNames = {"user_extension_id", "organization_id"}),
                @UniqueConstraint(
                        name = "uq_memberships_user_group",
                        columnNames = {"keycloak_user_id", "keycloak_group_path"})
        })
public class Membership extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "branch_id", length = 36)
    private String branchId;

    @Column(name = "branch_name")
    private String branchName;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    @NotNull
    private Boolean isPrimary = false;

    @Column(name = "keycloak_group_path", nullable = false)
    @NotBlank
    @Size(max = 255)
    private String keycloakGroupPath;

    @Column(name = "keycloak_user_id", nullable = false, length = 36)
    @NotBlank
    private String keycloakUserId;

    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;

    @Column(name = "organization_name", nullable = false)
    private String organizationName;

    @Builder.Default
    @Column(name = "roles", columnDefinition = "text[]", nullable = false)
    @JdbcTypeCode(SqlTypes.ARRAY)
    private Set<String> roles = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_extension_id", nullable = false)
    @NotNull
    private UserExtension userExtension;

    public void markAsPrimary() {
        this.isPrimary = true;
    }

    public void clearPrimary() {
        this.isPrimary = false;
    }

    public void updateRoles(Set<String> roles) {
        this.roles = roles != null ? roles : new HashSet<>();
    }
}
