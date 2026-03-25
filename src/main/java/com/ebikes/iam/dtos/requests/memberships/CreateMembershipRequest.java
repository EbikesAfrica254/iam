package com.ebikes.iam.dtos.requests.memberships;

import com.ebikes.iam.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Set;

public record CreateMembershipRequest(
        String branchId,
        String branchName,
        @NotNull Boolean isPrimary,
        @NotBlank String organizationId,
        @NotBlank String organizationName,
        @NotEmpty Set<UserRole> roles)
        implements Serializable {

    public CreateMembershipRequest {
        roles = roles != null ? Set.copyOf(roles) : Set.of();
    }
}
