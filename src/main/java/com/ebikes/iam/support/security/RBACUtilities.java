package com.ebikes.iam.support.security;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.ebikes.iam.enums.RoleScope;
import com.ebikes.iam.enums.UserRole;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RBACUtilities {

  private static final Set<UserRole> ADMIN_ROLES =
      Set.of(UserRole.BRANCH_ADMIN, UserRole.ORGANIZATION_ADMIN, UserRole.SYSTEM_ADMIN);

  private static final Map<UserRole, RoleMetadata> ROLE_METADATA =
      Map.ofEntries(
          // System
          entry(UserRole.SYSTEM_ADMIN, 100, 100, RoleScope.SYSTEM),

          // Organization
          entry(UserRole.ORGANIZATION_ADMIN, 80, 80, RoleScope.ORGANIZATION),
          entry(UserRole.ORGANIZATION_CHECKER, 40, 60, RoleScope.ORGANIZATION),
          entry(UserRole.ORGANIZATION_FLEET_MANAGER, 40, 60, RoleScope.ORGANIZATION),
          entry(UserRole.ORGANIZATION_FLEET_SUPPORT, 40, 60, RoleScope.ORGANIZATION),
          entry(UserRole.ORGANIZATION_INVENTORY_MANAGER, 40, 60, RoleScope.ORGANIZATION),
          entry(UserRole.ORGANIZATION_MAKER, 40, 60, RoleScope.ORGANIZATION),
          entry(UserRole.ORGANIZATION_OPERATOR, 40, 60, RoleScope.ORGANIZATION),

          // Branch
          entry(UserRole.AGENT, 20, 40, RoleScope.BRANCH),
          entry(UserRole.BRANCH_ADMIN, 60, 80, RoleScope.BRANCH),
          entry(UserRole.BRANCH_CHECKER, 20, 40, RoleScope.BRANCH),
          entry(UserRole.BRANCH_FLEET_MANAGER, 20, 40, RoleScope.BRANCH),
          entry(UserRole.BRANCH_FLEET_SUPPORT, 20, 40, RoleScope.BRANCH),
          entry(UserRole.BRANCH_INVENTORY_MANAGER, 20, 40, RoleScope.BRANCH),
          entry(UserRole.BRANCH_MAKER, 20, 40, RoleScope.BRANCH),
          entry(UserRole.BRANCH_OPERATOR, 20, 40, RoleScope.BRANCH),

          // Public
          entry(UserRole.CUSTOMER, 0, 40, RoleScope.PUBLIC));

  public static int getHighestAuthority(Set<UserRole> roles) {
    return roles.stream().mapToInt(role -> metadata(role).authority()).max().orElse(0);
  }

  public static int getRequiredAuthorityToCreate(UserRole targetRole) {
    return metadata(targetRole).requiredAuthorityToCreate();
  }

  public static boolean hasAdminRole(Set<UserRole> roles) {
    return roles.stream().anyMatch(ADMIN_ROLES::contains);
  }

  public static boolean hasAdminRoleFromNames(Set<String> roleNames) {
    return hasAdminRole(parseRoles(roleNames));
  }

  public static boolean isBranchRole(UserRole role) {
    return metadata(role).scope() == RoleScope.BRANCH;
  }

  /**
   * Returns true for ORGANIZATION and SYSTEM scoped roles. SYSTEM_ADMIN is intentionally included
   * as it operates at the organization level during user provisioning scope checks.
   */
  public static boolean isOrganizationRole(UserRole role) {
    RoleScope scope = metadata(role).scope();
    return scope == RoleScope.ORGANIZATION || scope == RoleScope.SYSTEM;
  }

  public static Set<UserRole> parseRoles(Set<String> roleNames) {
    return roleNames.stream()
        .map(UserRole::fromString)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private static Map.Entry<UserRole, RoleMetadata> entry(
      UserRole role, int authority, int requiredAuthorityToCreate, RoleScope scope) {
    return Map.entry(role, new RoleMetadata(authority, requiredAuthorityToCreate, scope));
  }

  private static RoleMetadata metadata(UserRole role) {
    RoleMetadata meta = ROLE_METADATA.get(role);
    if (meta == null) {
      throw new IllegalStateException("No RBAC metadata defined for role: " + role);
    }
    return meta;
  }

  private record RoleMetadata(int authority, int requiredAuthorityToCreate, RoleScope scope) {}
}
