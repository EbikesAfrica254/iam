package com.ebikes.iam.database.entities;

import java.io.Serial;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.ebikes.iam.database.entities.bases.SoftDeletableEntity;
import com.ebikes.iam.dtos.requests.users.UpdateUserExtensionRequest;
import com.ebikes.iam.enums.UserStatus;

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
    name = "user_extensions",
    schema = "iam",
    indexes = {
      @Index(name = "idx_user_ext_email", columnList = "email"),
      @Index(name = "idx_user_ext_status", columnList = "status")
    },
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_user_extensions_keycloak_user_id",
          columnNames = {"keycloak_user_id"}),
      @UniqueConstraint(
          name = "uq_user_extensions_username",
          columnNames = {"username"}),
      @UniqueConstraint(
          name = "uq_user_extensions_email",
          columnNames = {"email"}),
      @UniqueConstraint(
          name = "uq_user_extensions_phone_number",
          columnNames = {"phone_number"})
    })
public class UserExtension extends SoftDeletableEntity {

  @Serial private static final long serialVersionUID = 1L;

  @Column(name = "branch_id")
  private String branchId;

  @Column(name = "country_code", nullable = false, length = 2)
  @NotBlank @Size(min = 2, max = 2) private String countryCode;

  @Column(name = "email", nullable = false)
  @Email @NotBlank @Size(max = 255) private String email;

  @Builder.Default
  @Column(name = "email_verified", nullable = false, columnDefinition = "boolean")
  private boolean emailVerified = false;

  @Column(name = "first_name", nullable = false, length = 100)
  @NotBlank @Size(max = 100) private String firstName;

  @Column(name = "keycloak_user_id", nullable = false, length = 36)
  @NotBlank @Size(max = 36) private String keycloakUserId;

  @Column(name = "last_name", nullable = false, length = 100)
  @NotBlank @Size(max = 100) private String lastName;

  @OneToMany(mappedBy = "userExtension", cascade = CascadeType.ALL, orphanRemoval = true)
  private final Set<Membership> memberships = new HashSet<>();

  @Column(name = "organization_id", nullable = false, length = 36)
  private String organizationId;

  @Column(name = "phone_number", nullable = false, length = 50)
  @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{1,14}$") @Size(max = 50) private String phoneNumber;

  @Builder.Default
  @Column(name = "phone_number_verified", nullable = false, columnDefinition = "boolean")
  private boolean phoneNumberVerified = false;

  @Column(name = "status", columnDefinition = "iam.user_status", nullable = false)
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @NotNull private UserStatus status;

  @Column(name = "username", nullable = false)
  @NotBlank @Size(max = 100) private String username;

  @Column(nullable = false)
  @Version
  private Long version;

  public void activate() {
    this.status = UserStatus.ACTIVE;
    this.emailVerified = true;
  }

  public void delete() {
    this.softDelete();
    this.status = UserStatus.DELETED;
  }

  public void restore() {
    if (!isDeleted()) {
      throw new IllegalStateException("User not deleted");
    }
    this.deletedAt = null;
    this.status =
        (this.emailVerified || this.phoneNumberVerified) ? UserStatus.ACTIVE : UserStatus.INACTIVE;
  }

  public void update(UpdateUserExtensionRequest request) {
    if (request.email() != null) {
      this.email = request.email();
    }
    if (request.firstName() != null) {
      this.firstName = request.firstName();
    }
    if (request.lastName() != null) {
      this.lastName = request.lastName();
    }
    if (request.phoneNumber() != null) {
      this.phoneNumber = request.phoneNumber();
    }
    this.emailVerified = request.emailVerified();
    this.phoneNumberVerified = request.phoneNumberVerified();
  }

  public void updateStatus(UserStatus newStatus) {
    this.status = newStatus;
  }

  public void verifyEmail() {
    this.emailVerified = true;
  }

  public void verifyPhone() {
    this.phoneNumberVerified = true;
  }
}
