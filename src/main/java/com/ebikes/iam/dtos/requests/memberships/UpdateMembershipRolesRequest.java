package com.ebikes.iam.dtos.requests.memberships;

import com.ebikes.iam.enums.UserRole;
import jakarta.validation.constraints.NotEmpty;

import java.io.Serializable;
import java.util.Set;

public record UpdateMembershipRolesRequest(@NotEmpty Set<UserRole> roles) implements Serializable {
    public UpdateMembershipRolesRequest {
        roles = roles != null ? Set.copyOf(roles) : Set.of();
    }
}
