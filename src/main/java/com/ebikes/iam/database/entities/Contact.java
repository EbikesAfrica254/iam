package com.ebikes.iam.database.entities;

import java.io.Serial;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.ebikes.iam.database.entities.bases.BaseEntity;
import com.ebikes.iam.enums.ContactSourceType;
import com.ebikes.iam.enums.ContactStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(
    name = "contacts",
    schema = "iam",
    indexes = {
      @Index(name = "idx_contacts_branch_id", columnList = "branch_id"),
      @Index(name = "idx_contacts_organization_id", columnList = "organization_id"),
      @Index(name = "idx_contacts_phone_number", columnList = "phone_number"),
      @Index(name = "idx_contacts_source_ref", columnList = "source_reference"),
      @Index(name = "idx_contacts_status", columnList = "status")
    },
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_contacts_phone_org",
          columnNames = {"phone_number", "organization_id"})
    })
public class Contact extends BaseEntity {

  @Serial private static final long serialVersionUID = 1L;

  @Column(name = "branch_id", length = 36)
  private String branchId;

  @Column(name = "expires_at", nullable = false)
  @NotNull private OffsetDateTime expiresAt;

  @Column(name = "organization_id", nullable = false, length = 36)
  @NotBlank @Size(max = 36) private String organizationId;

  @Column(name = "phone_number", nullable = false, length = 20)
  @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{1,14}$") @Size(max = 20) private String phoneNumber;

  @Column(name = "source_reference", nullable = false)
  @NotBlank @Size(max = 255) private String sourceReference;

  @Column(name = "source_type", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  @NotNull private ContactSourceType sourceType;

  @Builder.Default
  @Column(name = "status", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  @NotNull private ContactStatus status = ContactStatus.UNRESOLVED;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_extension_id", referencedColumnName = "id")
  private UserExtension userExtension;

  @Version private Long version;

  public void claim(UserExtension userExtension) {
    if (this.status != ContactStatus.UNRESOLVED) {
      throw new IllegalStateException(
          "Cannot claim a contact that is not UNRESOLVED: current status=" + this.status);
    }
    this.userExtension = userExtension;
    this.status = ContactStatus.CLAIMED;
  }

  public void expire() {
    if (this.status != ContactStatus.UNRESOLVED) {
      throw new IllegalStateException(
          "Cannot expire a contact that is not UNRESOLVED: current status=" + this.status);
    }
    this.status = ContactStatus.EXPIRED;
  }
}
