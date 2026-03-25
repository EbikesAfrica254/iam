package com.ebikes.iam.dtos.requests.memberships;

import java.io.Serializable;
import java.util.Set;

import jakarta.validation.constraints.NotEmpty;

import com.ebikes.iam.enums.UserRole;

public record UpdateMembershipRolesRequest(@NotEmpty Set<UserRole> roles) implements Serializable {
  public UpdateMembershipRolesRequest {
    roles = roles != null ? Set.copyOf(roles) : Set.of();
  }
}
