package com.ebikes.iam.database.specifications;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.domain.Specification;

import com.ebikes.iam.database.entities.Contact;
import com.ebikes.iam.database.entities.UserExtension;
import com.ebikes.iam.enums.ResponseCode;
import com.ebikes.iam.enums.UserRole;
import com.ebikes.iam.exceptions.AuthorizationException;
import com.ebikes.iam.support.context.ExecutionContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuthorizationSpecifications {

  private static final String FIELD_BRANCH_ID = "branchId";
  private static final String FIELD_KEYCLOAK_USER_ID = "keycloakUserId";
  private static final String FIELD_ORGANIZATION_ID = "organizationId";

  private AuthorizationSpecifications() {
    // prevent instantiation
  }

  public static Specification<Contact> forContacts() {
    Set<String> roles = ExecutionContext.getRoles();
    Optional<Specification<Contact>> upperTier = resolveUpperTierFilter(roles);
    if (upperTier.isPresent()) {
      return upperTier.get();
    }

    log.error("Insufficient role for contact access: roles={}", roles);
    throw new AuthorizationException(
        ResponseCode.FORBIDDEN,
        "Contacts are only accessible to organisation-level roles and above");
  }

  public static Specification<UserExtension> forUserExtensions() {
    return applyOrganizationScopedFilter();
  }

  private static <T> Specification<T> applyOrganizationScopedFilter() {
    Set<String> roles = ExecutionContext.getRoles();

    Optional<Specification<T>> upperTier = resolveUpperTierFilter(roles);
    if (upperTier.isPresent()) {
      return upperTier.get();
    }

    String activeOrganization = validateActiveOrganization();

    if (hasAnyRole(
        roles,
        UserRole.BRANCH_ADMIN,
        UserRole.BRANCH_CHECKER,
        UserRole.BRANCH_FLEET_MANAGER,
        UserRole.BRANCH_FLEET_SUPPORT,
        UserRole.BRANCH_INVENTORY_MANAGER,
        UserRole.BRANCH_MAKER,
        UserRole.BRANCH_OPERATOR)) {
      String activeBranch = validateActiveBranch();
      log.debug(
          "BRANCH-level access: filtering by organizationId={}, branchId={}",
          activeOrganization,
          activeBranch);
      return Specification.allOf(
          filterByOrganizationId(activeOrganization), filterByBranchId(activeBranch));
    }

    if (hasAnyRole(roles, UserRole.AGENT, UserRole.CUSTOMER)) {
      String userId = validateUserId();
      log.debug(
          "USER-level access: filtering by organizationId={}, userId={}",
          activeOrganization,
          userId);
      return Specification.allOf(
          filterByOrganizationId(activeOrganization), filterByKeycloakUserId(userId));
    }

    log.error("No applicable role found for user with roles: {}", roles);
    throw new AuthorizationException(
        ResponseCode.FORBIDDEN, "Insufficient permissions for this operation");
  }

  private static <T> Optional<Specification<T>> resolveUpperTierFilter(Set<String> roles) {
    if (hasAnyRole(roles, UserRole.SYSTEM_ADMIN)) {
      log.debug("SYSTEM_ADMIN access: returning all records without filtering");
      return Optional.of(noFilter());
    }

    String activeOrganization = validateActiveOrganization();

    if (hasAnyRole(
        roles,
        UserRole.ORGANIZATION_ADMIN,
        UserRole.ORGANIZATION_CHECKER,
        UserRole.ORGANIZATION_FLEET_MANAGER,
        UserRole.ORGANIZATION_FLEET_SUPPORT,
        UserRole.ORGANIZATION_INVENTORY_MANAGER,
        UserRole.ORGANIZATION_OPERATOR)) {
      log.debug("ORGANIZATION-level access: filtering by organizationId={}", activeOrganization);
      return Optional.of(filterByOrganizationId(activeOrganization));
    }

    return Optional.empty();
  }

  private static <T> Specification<T> filterByBranchId(String branchId) {
    return (root, query, cb) -> cb.equal(root.get(FIELD_BRANCH_ID), branchId);
  }

  private static <T> Specification<T> filterByKeycloakUserId(String keycloakUserId) {
    return (root, query, cb) -> cb.equal(root.get(FIELD_KEYCLOAK_USER_ID), keycloakUserId);
  }

  private static <T> Specification<T> filterByOrganizationId(String organizationId) {
    return (root, query, cb) -> cb.equal(root.get(FIELD_ORGANIZATION_ID), organizationId);
  }

  private static <T> Specification<T> noFilter() {
    return (root, query, cb) -> cb.conjunction();
  }

  private static boolean hasAnyRole(Set<String> roles, UserRole... candidates) {
    for (UserRole candidate : candidates) {
      if (roles.contains(candidate.name())) {
        return true;
      }
    }
    return false;
  }

  private static String validateActiveBranch() {
    String activeBranch = ExecutionContext.getActiveBranch();
    if (activeBranch == null || activeBranch.isBlank()) {
      log.error("Missing active_branch claim");
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN, "Active branch context required for this operation");
    }
    return activeBranch;
  }

  private static String validateActiveOrganization() {
    String activeOrganization = ExecutionContext.getActiveOrganization();
    if (activeOrganization == null || activeOrganization.isBlank()) {
      log.error("Missing active_organization claim");
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN, "Active organization context required for this operation");
    }
    return activeOrganization;
  }

  private static String validateUserId() {
    String userId = ExecutionContext.getUserId();
    if (userId == null || userId.isBlank()) {
      log.error("Missing user_id claim for user-level access");
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN, "User identity required for this operation");
    }
    return userId;
  }
}
