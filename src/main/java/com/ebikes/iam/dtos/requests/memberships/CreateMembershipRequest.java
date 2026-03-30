package com.ebikes.iam.dtos.requests.memberships;

import java.io.Serializable;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.ebikes.iam.enums.UserRole;

public record CreateMembershipRequest(
    String branchId,
    @NotNull Boolean isPrimary,
    @NotBlank String organizationId,
    @NotEmpty Set<UserRole> roles)
    implements Serializable {

  public CreateMembershipRequest {
    roles = roles != null ? Set.copyOf(roles) : Set.of();
  }
}
