package com.ebikes.iam.services.users;

import com.ebikes.iam.database.entities.Membership;
import com.ebikes.iam.enums.ResponseCode;
import com.ebikes.iam.enums.UserRole;
import com.ebikes.iam.exceptions.AuthorizationException;
import com.ebikes.iam.support.context.ExecutionContext;
import com.ebikes.iam.support.security.RBACUtilities;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ebikes.iam.support.security.RBACUtilities.getRequiredAuthorityToCreate;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserCreationAuthorizationService {

  private final MembershipService membershipService;

  /**
   * Validates that the requesting user has enough authority to create a user with the given roles
   * in the target organization and branch.
   *
   * <p>Branch context is read from {@link ExecutionContext#getActiveBranch()}, which is populated
   * by the incoming HTTP request. This method must not be called from async or batch contexts where
   * the request-scoped state is unavailable.
   *
   * @param creatorUserId        Keycloak user ID of the creator
   * @param targetBranchId       branch the new user will be assigned to, or null for org-level
   * @param targetOrganizationId organization in which the user is being created
   * @param targetRoles          roles to be assigned to the new user
   */
  @Transactional(readOnly = true)
  public void authorize(
      String creatorUserId,
      String targetBranchId,
      String targetOrganizationId,
      Set<UserRole> targetRoles) {

    String creatorBranchId = ExecutionContext.getActiveBranch();

    Membership creatorMembership =
        membershipService
            .findMembershipInScope(creatorBranchId, creatorUserId, targetOrganizationId)
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
