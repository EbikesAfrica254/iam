package com.ebikes.iam.services.users.provisioning;

import static com.ebikes.iam.support.security.RBACUtilities.getRequiredAuthorityToCreate;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ebikes.iam.database.entities.Membership;
import com.ebikes.iam.enums.ResponseCode;
import com.ebikes.iam.enums.UserRole;
import com.ebikes.iam.exceptions.AuthorizationException;
import com.ebikes.iam.services.users.membership.MembershipService;
import com.ebikes.iam.support.context.ExecutionContext;
import com.ebikes.iam.support.security.RBACUtilities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class AuthorizationService {

  private final MembershipService membershipService;

  public void authorize(
      String targetBranchId, String targetOrganizationId, Set<UserRole> targetRoles) {

    if (!(ExecutionContext.get() instanceof ExecutionContext.UserContext ctx)) {
      return;
    }

    if (ExecutionContext.get() instanceof ExecutionContext.UserContext uc
        && uc.roles().contains(UserRole.SYSTEM_ADMIN.name())) {
      return;
    }

    String creatorBranchId = ctx.activeBranch();

    Membership creatorMembership =
        membershipService
            .findMembershipInScope(creatorBranchId, ctx.userId(), targetOrganizationId)
            .orElseThrow(
                () ->
                    new AuthorizationException(
                        ResponseCode.FORBIDDEN,
                        "You do not have permission to create users in this organization"));

    Set<String> creatorRoles = creatorMembership.getRoles();

    if (creatorRoles.isEmpty()) {
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN, "You do not have any roles in this organization");
    }

    Set<UserRole> creatorRoleEnums =
        creatorRoles.stream()
            .map(UserRole::fromString)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    int creatorAuthority = RBACUtilities.getHighestAuthority(creatorRoleEnums);

    for (UserRole targetRole : targetRoles) {
      int requiredAuthority = getRequiredAuthorityToCreate(targetRole);
      if (creatorAuthority < requiredAuthority) {
        throw new AuthorizationException(
            ResponseCode.FORBIDDEN,
            "You do not have sufficient permissions to assign the role: " + targetRole);
      }
    }

    if (creatorRoleEnums.contains(UserRole.BRANCH_ADMIN)
        && !creatorRoleEnums.contains(UserRole.ORGANIZATION_ADMIN)
        && !creatorRoleEnums.contains(UserRole.SYSTEM_ADMIN)) {

      if (targetBranchId == null) {
        throw new AuthorizationException(
            ResponseCode.FORBIDDEN, "Branch administrators cannot create organization-level users");
      }

      if (creatorBranchId == null || !creatorBranchId.equals(targetBranchId)) {
        throw new AuthorizationException(
            ResponseCode.FORBIDDEN,
            "You can only create users in your own branch: " + creatorBranchId);
      }
    }
  }
}
